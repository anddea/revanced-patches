package app.morphe.patches.shared.audiotracks

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatchBuilder
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import app.morphe.patches.shared.AUDIO_TRACK_DISPLAY_NAME_STRING
import app.morphe.patches.shared.AUDIO_TRACK_ID_STRING
import app.morphe.patches.shared.IS_DEFAULT_AUDIO_TRACK_STRING
import app.morphe.patches.shared.extension.Constants.PATCHES_PATH
import app.morphe.patches.shared.formatStreamModelToStringFingerprint
import app.morphe.util.addInstructionsAtControlFlowLabel
import app.morphe.util.cloneMutable
import app.morphe.util.findMethodFromToString
import app.morphe.util.fingerprint.injectLiteralInstructionBooleanCall
import app.morphe.util.fingerprint.originalMethodOrThrow
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.immutable.ImmutableField

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$PATCHES_PATH/AutoAudioTracksPatch;"

/**
 * Patch shared with YouTube and YT Music.
 */
internal fun audioTracksPatch(
    block: BytecodePatchBuilder.() -> Unit = {},
    executeBlock: BytecodePatchContext.() -> Unit = {},
    fixUseLocalizedAudioTrackFlag: Boolean,
) = bytecodePatch(
    name = "Disable forced auto audio tracks",
    description = "Adds an option to disable audio tracks from being automatically enabled.",
) {

    block()

    execute {
        // Disable feature flag that ignores the default track flag
        // and instead overrides to the user region language.
        if (fixUseLocalizedAudioTrackFlag) {
            selectAudioStreamFingerprint.injectLiteralInstructionBooleanCall(
                AUDIO_STREAM_IGNORE_DEFAULT_FEATURE_FLAG,
                "$EXTENSION_CLASS_DESCRIPTOR->ignoreDefaultAudioStream(Z)Z"
            )
        }

        val toStringMethod = formatStreamModelToStringFingerprint.originalMethodOrThrow()
        val isDefaultAudioTrackMethod = toStringMethod
            .findMethodFromToString(IS_DEFAULT_AUDIO_TRACK_STRING)
        val audioTrackDisplayNameMethod = toStringMethod
            .findMethodFromToString(AUDIO_TRACK_DISPLAY_NAME_STRING)
        val audioTrackIdMethod = toStringMethod
            .findMethodFromToString(AUDIO_TRACK_ID_STRING)

        mutableClassDefBy {
            it.type == audioTrackIdMethod.definingClass
        }.apply {
            // Add a new field to store the override.
            val helperFieldName = "isDefaultAudioTrackOverride"
            fields.add(
                ImmutableField(
                    type,
                    helperFieldName,
                    "Ljava/lang/Boolean;",
                    // Boolean is a 100% immutable class (all fields are final)
                    // and safe to write to a shared field without volatile/synchronization,
                    // but without volatile the field can show stale data
                    // and the same field is calculated more than once by different threads.
                    AccessFlags.PRIVATE.value or AccessFlags.VOLATILE.value,
                    null,
                    annotations,
                    null
                ).toMutable()
            )

            // Clone the method to add additional registers because the
            // isDefaultAudioTrack() has only 1 or 2 registers and 3 are needed.
            val originalRegisterCount = isDefaultAudioTrackMethod.implementation!!.registerCount
            val originalP0Register = originalRegisterCount - 1
            val clonedMethod = isDefaultAudioTrackMethod.cloneMutable(
                registerCount = originalRegisterCount + 4
            ).apply {
                // Preserve the original p0 register after increasing the register count.
                addInstruction(0, "move-object/from16 v$originalP0Register, p0")
            }

            // Replace existing method with cloned with more registers.
            methods.apply {
                remove(isDefaultAudioTrackMethod)
                add(clonedMethod)
            }

            clonedMethod.apply {
                // Free registers are added.
                val free1 = originalRegisterCount + 1
                val free2 = free1 + 1
                val insertIndex = indexOfFirstInstructionReversedOrThrow(Opcode.RETURN)
                val originalResultRegister =
                    getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstructionsAtControlFlowLabel(
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
    
                        invoke-static { v$originalResultRegister, v$free1, v$free2 }, $EXTENSION_CLASS_DESCRIPTOR->isDefaultAudioStream(ZLjava/lang/String;Ljava/lang/String;)Z
                        move-result v$free1
                        
                        invoke-static { v$free1 }, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;
                        move-result-object v$free2
                        iput-object v$free2, p0, $type->$helperFieldName:Ljava/lang/Boolean;
                        return v$free1
                        """
                )
            }
        }

        executeBlock()
    }
}
