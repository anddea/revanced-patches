package app.morphe.patches.shared.drc

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatchBuilder
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patches.shared.extension.Constants.PATCHES_PATH
import app.morphe.patches.shared.formatStreamModelConstructorFingerprint
import app.morphe.util.addInstructionsAtControlFlowLabel
import app.morphe.util.fingerprint.injectLiteralInstructionBooleanCall
import app.morphe.util.Utils.printWarn
import app.morphe.util.fingerprint.matchOrThrow
import app.morphe.util.fingerprint.methodOrNull
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

internal const val EXTENSION_CLASS_DESCRIPTOR =
    "$PATCHES_PATH/DrcAudioPatch;"

fun drcAudioPatch(
    useLatestFingerprint: BytecodePatchBuilder.() -> Boolean = { false },
) = bytecodePatch(
    description = "drcAudioPatch",
) {
    execute {
        val fingerprint = if (useLatestFingerprint()) {
            compressionRatioFingerprint
        } else {
            compressionRatioLegacyFingerprint
        }

        val (formatFieldReference, loudnessDbFieldReference) =
            fingerprint.matchOrThrow(formatStreamModelConstructorFingerprint).let {
                with(it.method) {
                    val loudnessDbIndex = it.instructionMatches.first().index + 1
                    val loudnessDbFieldReference =
                        getInstruction<ReferenceInstruction>(loudnessDbIndex).reference as FieldReference
                    val formatClass = loudnessDbFieldReference.definingClass
                    val formatField = it.classDef.fields.find { field ->
                        field.type == formatClass
                    } ?: throw PatchException("Failed to find format field")

                    Pair(formatField, loudnessDbFieldReference)
                }
            }

        formatStreamModelConstructorFingerprint.matchOrThrow().let {
            it.method.apply {
                val helperMethodName = "patch_setLoudnessDb"

                it.classDef.methods.add(
                    ImmutableMethod(
                        it.classDef.type,
                        helperMethodName,
                        emptyList(),
                        "V",
                        AccessFlags.PRIVATE.value or AccessFlags.FINAL.value,
                        annotations,
                        null,
                        MutableMethodImplementation(3),
                    ).toMutable().apply {
                        addInstructionsWithLabels(
                            0,
                            """
                            invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->disableDrcAudio()Z
                            move-result v0
                            if-eqz v0, :exit
                            iget-object v0, p0, $formatFieldReference
                            const/4 v1, 0x0
                            iput v1, v0, $loudnessDbFieldReference
                            iput-object v0, p0, $formatFieldReference
                            :exit
                            return-void
                            """,
                        )
                    }
                )

                val insertIndex = implementation!!.instructions.lastIndex

                addInstructionsAtControlFlowLabel(
                    insertIndex,
                    "invoke-direct/range { p0 .. p0 }, $definingClass->$helperMethodName()V"
                )
            }
        }

        // modified by lavinhoque33, 2026-10-07: when the flag is gone (YouTube 21.39 / YT Music 9.40),
        // null the first argument of the unconditional loudness method so it returns a neutral result.
        val flagHooked = volumeNormalizationConfigFingerprint.injectLiteralInstructionBooleanCall(
            VOLUME_NORMALIZATION_EXPERIMENTAL_FEATURE_FLAG,
            "$EXTENSION_CLASS_DESCRIPTOR->disableDrcAudioFeatureFlag(Z)Z",
            warnIfMissing = false
        )

        if (!flagHooked) {
            val method = volumeNormalizationMethodFingerprint.methodOrNull()
            if (method == null) {
                // modified by lavinhoque33, 2026-10-08: user-facing wording.
                printWarn("\"Disable DRC audio\" is not supported in this version. Use YouTube versions up to 21.39 or YouTube Music versions up to 9.40.")
            } else {
                method.addInstructionsWithLabels(
                    3,
                    """
                    invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->disableDrcAudio()Z
                    move-result v0
                    if-eqz v0, :patch_drc_skip
                    const/4 v1, 0x0
                    :patch_drc_skip
                    nop
                    """,
                )
            }
        }
    }
}