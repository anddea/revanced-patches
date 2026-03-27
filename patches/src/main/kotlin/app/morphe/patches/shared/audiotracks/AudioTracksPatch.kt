package app.morphe.patches.shared.audiotracks

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatchBuilder
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patches.shared.AUDIO_TRACK_DISPLAY_NAME_STRING
import app.morphe.patches.shared.AUDIO_TRACK_ID_STRING
import app.morphe.patches.shared.IS_DEFAULT_AUDIO_TRACK_STRING
import app.morphe.patches.shared.extension.Constants.PATCHES_PATH
import app.morphe.patches.shared.formatStreamModelToStringFingerprint
import app.morphe.util.findMethodFromToString
import app.morphe.util.fingerprint.injectLiteralInstructionBooleanCall
import app.morphe.util.fingerprint.originalMethodOrThrow
import app.morphe.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.immutable.ImmutableField
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

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

            // Add a helper method because the isDefaultAudioTrack() has only 2 registers and 3 are needed.
            val helperMethodClass = type
            val helperMethodName = "patch_isDefaultAudioTrack"
            val helperMethod = ImmutableMethod(
                helperMethodClass,
                helperMethodName,
                listOf(ImmutableMethodParameter("Z", annotations, null)),
                "Z",
                AccessFlags.PRIVATE.value,
                null,
                null,
                MutableMethodImplementation(6),
            ).toMutable().apply {
                addInstructionsWithLabels(
                    0,
                    """
                        iget-object v0, p0, $helperMethodClass->$helperFieldName:Ljava/lang/Boolean;
                        if-eqz v0, :call_extension            
                        invoke-virtual { v0 }, Ljava/lang/Boolean;->booleanValue()Z
                        move-result v3
                        return v3
                        
                        :call_extension
                        invoke-virtual { p0 }, $audioTrackIdMethod
                        move-result-object v1
                        
                        invoke-virtual { p0 }, $audioTrackDisplayNameMethod
                        move-result-object v2
    
                        invoke-static { p1, v1, v2 }, $EXTENSION_CLASS_DESCRIPTOR->isDefaultAudioStream(ZLjava/lang/String;Ljava/lang/String;)Z
                        move-result v3
                        
                        invoke-static { v3 }, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;
                        move-result-object v0
                        iput-object v0, p0, $helperMethodClass->$helperFieldName:Ljava/lang/Boolean;
                        return v3
                        """
                )
            }
            methods.add(helperMethod)

            // Modify isDefaultAudioTrack() to call extension helper method.
            isDefaultAudioTrackMethod.apply {
                val index = indexOfFirstInstructionOrThrow(Opcode.RETURN)
                val register = getInstruction<OneRegisterInstruction>(index).registerA

                addInstructions(
                    index,
                    """
                        invoke-direct { p0, v$register }, $helperMethodClass->$helperMethodName(Z)Z
                        move-result v$register
                        """
                )
            }
        }

        executeBlock()
    }
}
