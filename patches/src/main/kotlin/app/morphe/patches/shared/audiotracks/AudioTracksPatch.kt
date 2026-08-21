/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
 * - inotia00 (https://github.com/inotia00)
 *
 * Licensed under the GNU General Public License v3.0.
 *
 * ------------------------------------------------------------------------
 * GPLv3 Section 7 – Additional Terms & Attribution Requirements
 * ------------------------------------------------------------------------
 *
 * This file contains substantial original work by the author(s) listed above.
 *
 * In accordance with Section 7 of the GNU General Public License v3.0,
 * the following additional terms apply to this file:
 *
 * 1. Source Credit Preservation (Section 7(b)): This specific copyright notice
 *    and the list of original authors above must be preserved in any copy
 *    or derivative work. You may add your own copyright notice below it,
 *    but you may not remove the original one.
 *
 * 2. Origin & Modification Marking (Section 7(c)): Modified versions must be
 *    clearly marked as such (e.g., by adding a "Modified by" line or a new
 *    copyright notice) and must not be misrepresented as the original work.
 *
 * 3. Version Control Attribution (Section 7(b)): Any ports or substantial
 *    modifications must retain historical authorship credit in version control
 *    systems (e.g., Git), listing original author(s) appropriately and
 *    modifiers as committers or co-authors.
 *
 * 4. User Interface Attribution (Section 7(b)): Any works containing or
 *    derived from this material must maintain a visible credit or
 *    acknowledgment to the original author(s) within the application's
 *    user interface (e.g., in an "About" or "Credits" section).
 */

/*
 * Portions of this file are ported from Morphe:
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.shared.audiotracks

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchBuilder
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patches.shared.mapping.resourceMappingPatch
import app.morphe.util.addInstructionsAtControlFlowLabel
import app.morphe.util.cloneMutable
import app.morphe.util.findMethodFromToString
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import app.morphe.util.insertLiteralOverride
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableField
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

private const val EXTENSION_CLASS =
    "Lapp/morphe/extension/shared/patches/ForceOriginalAudioPatch;"
private const val EXTENSION_AUDIO_TRACK_INTERFACE =
    $$"Lapp/morphe/extension/shared/patches/ForceOriginalAudioPatch$AudioTrackInterface;"

/**
 * Patch shared with YouTube and YT Music.
 */
internal fun audioTracksPatch(
    block: BytecodePatchBuilder.() -> Unit = {},
    executeBlock: BytecodePatchContext.() -> Unit = {},
    fixUseLocalizedAudioTrackFlag: BytecodePatchContext.() -> Boolean,
    forcedServerAdaptiveStreaming: BytecodePatchContext.() -> Boolean,
    mainActivityOnCreateFingerprint: Fingerprint,
    subclassExtensionClassDescriptor: String,
) = bytecodePatch(
    name = "Force original audio",
    description = "Adds an option to disable audio tracks from being automatically enabled.",
) {

    block()

    dependsOn(resourceMappingPatch)

    execute {
        FormatStreamModelToStringFingerprint.let {
            val isDefaultAudioTrackMethod = it.originalMethod.findMethodFromToString("isDefaultAudioTrack=")
            val audioTrackDisplayNameMethod = it.originalMethod.findMethodFromToString("audioTrackDisplayName=")
            val audioTrackIdMethod = it.originalMethod.findMethodFromToString("audioTrackId=")

            it.classDef.apply {
                // Add a new field to store the override.
                val helperFieldName = "patch_isDefaultAudioTrackOverride"
                fields.add(
                    ImmutableField(
                        type,
                        helperFieldName,
                        "Ljava/lang/Boolean;",
                        AccessFlags.PRIVATE.value or AccessFlags.VOLATILE.value,
                        null,
                        null,
                        null
                    ).toMutable()
                )

                val originalRegisterCount = isDefaultAudioTrackMethod.implementation!!.registerCount
                val clonedMethod = isDefaultAudioTrackMethod.cloneMutable(
                    registerCount = originalRegisterCount + 4
                )

                it.classDef.methods.apply {
                    remove(isDefaultAudioTrackMethod)
                    add(clonedMethod)
                }

                clonedMethod.apply {
                    val fieldRef = isDefaultAudioTrackMethod.getInstruction(0).getReference<FieldReference>()!!
                    replaceInstruction(0, "iget-object v0, p0, $fieldRef")

                    val free1 = originalRegisterCount + 1
                    val free2 = free1 + 1
                    val insertIndex = indexOfFirstInstructionReversedOrThrow(Opcode.RETURN)
                    val originalResultRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                    clonedMethod.addInstructionsAtControlFlowLabel(
                        insertIndex,
                        """
                            iget-object v$free1, p0, $type->$helperFieldName:Ljava/lang/Boolean;
                            if-eqz v$free1, :call_extension            
                            invoke-virtual { v$free1 }, Ljava/lang/Boolean;->booleanValue()Z
                            move-result v$free1
                            return v$free1
                            
                            :call_extension
                            invoke-virtual { p0 }, $audioTrackIdMethod
                            move-result-object v$free1
                            
                            invoke-virtual { p0 }, $audioTrackDisplayNameMethod
                            move-result-object v$free2
        
                            invoke-static { v$originalResultRegister, v$free1, v$free2 }, $EXTENSION_CLASS->isDefaultAudioStream(ZLjava/lang/String;Ljava/lang/String;)Z
                            move-result v$free1
                            
                            invoke-static { v$free1 }, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;
                            move-result-object v$free2
                            iput-object v$free2, p0, $type->$helperFieldName:Ljava/lang/Boolean;
                            return v$free1
                        """
                    )
                }
            }
        }

        // Disable feature flag that ignores the default track flag
        // and instead overrides to the user region language.
        if (fixUseLocalizedAudioTrackFlag()) {
            SelectAudioStreamFingerprint.method.insertLiteralOverride(
                SelectAudioStreamFingerprint.instructionMatches.first().index,
                "$EXTENSION_CLASS->ignoreDefaultAudioStream(Z)Z"
            )
        }

        // If there is no feature flag, the SABR protocol parameter (proto buffer) must be overridden:
        // https://github.com/LuanRT/googlevideo/commit/173a2b0717c19c922e5fb53b170640a9c9d58819
        //
        // Since mapping the proto field and finding the appropriate hooking point is very difficult,
        // 'Default audio track' patches has been implemented (like 'Default video quality' patches).
        if (forcedServerAdaptiveStreaming()) {
            val audioTrackRecordClass = with(AudioTrackRecordToStringFingerprint) {
                val definingClass = classDef.type
                mapOf(
                    0 to "patch_getId",
                    1 to "patch_getDisplayName",
                    3 to "patch_getIsDefault"
                ).forEach { (matchIndex, methodName) ->
                    val fieldInstruction = instructionMatches[matchIndex].instruction
                    val fieldOpcode = fieldInstruction.opcode.name
                    val fieldReference = fieldInstruction.getReference<FieldReference>()!!
                    val fieldReturnType = fieldReference.type
                    val operation = if (fieldReturnType == "Z") {
                        "return v0"
                    } else {
                        "return-object v0"
                    }

                    val helperMethod = ImmutableMethod(
                        definingClass,
                        methodName,
                        listOf(),
                        fieldReturnType,
                        AccessFlags.PUBLIC.value or AccessFlags.FINAL.value,
                        null,
                        null,
                        MutableMethodImplementation(2),
                    ).toMutable().apply {
                        addInstructions(
                            0,
                            """
                                $fieldOpcode v0, p0, $fieldReference
                                $operation
                            """
                        )
                    }

                    classDef.methods.add(helperMethod)
                }

                classDef.interfaces.add(EXTENSION_AUDIO_TRACK_INTERFACE)
                definingClass
            }

            val setAudioTrackMethod =
                getAudioTrackItemOnClickFingerprint(audioTrackRecordClass)
                    .instructionMatches.last().instruction.getReference<MethodReference>()!!

            val playerControllerClass = setAudioTrackMethod.definingClass

            val audioTrackRecordArrayField =
                getCurrentAudioFormatConstructorFingerprint(audioTrackRecordClass)
                    .instructionMatches.last().instruction.getReference<FieldReference>()!!

            getSetVideoQualityListFingerprint(
                audioVideoFormatClass = audioTrackRecordArrayField.definingClass,
                playerControllerClass = playerControllerClass
            ).let {
                it.method.apply {
                    val helperMethod = ImmutableMethod(
                        definingClass,
                        "patch_setAudioTrack",
                        listOf(
                            ImmutableMethodParameter(
                                "Ljava/lang/String;",
                                null,
                                null
                            )
                        ),
                        "V",
                        AccessFlags.PRIVATE.value or AccessFlags.FINAL.value,
                        null,
                        null,
                        MutableMethodImplementation(3),
                    ).toMutable().apply {
                        val playerControllerField = it.classDef.fields.single { field ->
                            field.type == playerControllerClass
                        }
                        addInstructionsWithLabels(
                            0,
                            """
                                # Check if the audio track id is null.
                                if-eqz p1, :ignore
                                iget-object v0, p0, $playerControllerField
                                
                                # Check if the player controller class is null.
                                if-eqz v0, :ignore
                                invoke-virtual { v0, p1 }, $setAudioTrackMethod
                                
                                :ignore
                                return-void
                            """
                        )
                    }

                    it.classDef.methods.add(helperMethod)

                    val index = it.instructionMatches.first().index
                    val instruction = getInstruction<TwoRegisterInstruction>(index)
                    val freeRegister = instruction.registerA
                    val audioVideoFormatRegister = instruction.registerB

                    addInstructionsAtControlFlowLabel(
                        index,
                        """
                            iget-object v$freeRegister, v$audioVideoFormatRegister, $audioTrackRecordArrayField
                            invoke-static { v$freeRegister }, $EXTENSION_CLASS->getDefaultAudioTrackId([$EXTENSION_AUDIO_TRACK_INTERFACE)Ljava/lang/String;
                            move-result-object v$freeRegister
                            invoke-direct { p0, v$freeRegister }, $helperMethod
                        """
                    )
                }
            }
        }

        mainActivityOnCreateFingerprint.method.addInstruction(
            0,
            "invoke-static { }, $subclassExtensionClassDescriptor->setEnabled()V"
        )

        executeBlock()
    }
}
