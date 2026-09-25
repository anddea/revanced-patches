/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.patches.youtube.general.spoofappversion

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patches.youtube.utils.extension.sharedExtensionPatch
import app.morphe.patches.youtube.utils.playservice.is_21_05_or_greater
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import app.morphe.util.addInstructionsAtControlFlowLabel
import app.morphe.util.cloneMutableAndPreserveParameters
import app.morphe.util.findInstructionIndicesReversedOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

private lateinit var clientInfoField: FieldReference
private lateinit var clientVersionField: FieldReference
private lateinit var messageLiteBuilderField: FieldReference
private lateinit var messageLiteBuilderMethod: MethodReference
private var contextHookInitialized = false

internal enum class Endpoint(
    vararg val parentFingerprints: Fingerprint,
    var smaliInstructions: String = "",
) {
    REEL(
        ReelCreateItemsEndpointConstructorFingerprint,
        ReelItemWatchEndpointConstructorFingerprint,
        ReelWatchSequenceEndpointConstructorFingerprint,
    ),
}

/**
 * Provides endpoint-specific hooks for fields shared by InnerTube request bodies.
 *
 * The global spoof-app-version hook is intentionally left in place for normal requests. Shorts
 * needs a separate hook because YouTube 21.05 and newer no longer contain the legacy overlay that
 * the older Reel response selects.
 */
internal val clientContextHookPatch = bytecodePatch(
    description = "Hooks the context body of the Shorts endpoint.",
) {
    dependsOn(sharedExtensionPatch, versionCheckPatch)

    execute {
        contextHookInitialized = false
        Endpoint.entries.forEach { it.smaliInstructions = "" }

        // The endpoint-specific hook is only needed by the modern Shorts client.
        if (!is_21_05_or_greater) return@execute

        BuildDummyClientContextBodyFingerprint.let {
            it.method.apply {
                val clientInfoIndex = it.instructionMatches.last().index
                val clientVersionIndex = it.instructionMatches[2].index
                val messageLiteBuilderIndex = it.instructionMatches.first().index

                clientInfoField =
                    getInstruction<ReferenceInstruction>(clientInfoIndex).reference as FieldReference
                clientVersionField =
                    getInstruction<ReferenceInstruction>(clientVersionIndex).reference as FieldReference
                messageLiteBuilderField =
                    getInstruction<ReferenceInstruction>(messageLiteBuilderIndex).reference as FieldReference
            }
        }

        AuthenticationChangeListenerFingerprint.method.apply {
            val messageLiteBuilderIndex =
                indexOfMessageLiteBuilderReference(this, messageLiteBuilderField.definingClass)

            messageLiteBuilderMethod =
                getInstruction<ReferenceInstruction>(messageLiteBuilderIndex).reference as MethodReference
        }

        contextHookInitialized = true
    }

    finalize {
        if (!contextHookInitialized) return@finalize

        Endpoint.entries.filter { it.smaliInstructions.isNotEmpty() }.forEach { endpoint ->
            endpoint.parentFingerprints.forEach { parentFingerprint ->
                // Use a local fingerprint because Fingerprint caches its match.
                val endpointRequestBodyFingerprint = Fingerprint(
                    classFingerprint = parentFingerprint,
                    accessFlags = listOf(AccessFlags.PROTECTED, AccessFlags.FINAL),
                    returnType = "V",
                    parameters = emptyList(),
                )

                endpointRequestBodyFingerprint.match(parentFingerprint.originalClassDef).let { match ->
                    // 21.05+ clobbers the p0 register while building the request body.
                    match.method.cloneMutableAndPreserveParameters(match.classDef).apply {
                        match.classDef.methods.add(
                            ImmutableMethod(
                                definingClass,
                                "patch_setClientContext",
                                emptyList(),
                                "V",
                                AccessFlags.PRIVATE.value or AccessFlags.FINAL.value,
                                annotations,
                                null,
                                MutableMethodImplementation(5),
                            ).toMutable().apply {
                                addInstructionsWithLabels(
                                    0,
                                    """
                                        invoke-virtual { p0 }, $messageLiteBuilderMethod
                                        move-result-object v0
                                        iget-object v0, v0, $messageLiteBuilderField
                                        check-cast v0, ${clientInfoField.definingClass}
                                        iget-object v1, v0, $clientInfoField
                                        if-eqz v1, :ignore
                                        ${endpoint.smaliInstructions}
                                        :ignore
                                        return-void
                                    """,
                                )
                            },
                        )

                        findInstructionIndicesReversedOrThrow(Opcode.RETURN_VOID).forEach { index ->
                            addInstructionsAtControlFlowLabel(
                                index,
                                "invoke-direct/range { p0 .. p0 }, " +
                                        "$definingClass->patch_setClientContext()V",
                            )
                        }
                    }
                }
            }
        }
    }
}

internal fun addClientVersionHook(endpoint: Endpoint, descriptor: String) {
    endpoint.smaliInstructions += """
        iget-object v2, v1, $clientVersionField
        invoke-static { v2 }, $descriptor
        move-result-object v2
        iput-object v2, v1, $clientVersionField
        """
}
