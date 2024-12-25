package app.revanced.patches.youtube.general.audiotracks

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.patch.PatchList.DISABLE_AUTO_AUDIO_TRACKS
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val audioTracksPatch = bytecodePatch(
    DISABLE_AUTO_AUDIO_TRACKS.title,
    DISABLE_AUTO_AUDIO_TRACKS.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)
    execute {


        streamingModelBuilderFingerprint.methodOrThrow().apply {
            val formatStreamModelIndex = indexOfFirstInstructionOrThrow {
                opcode == Opcode.CHECK_CAST
                        && (this as ReferenceInstruction).reference.toString() == "Lcom/google/android/libraries/youtube/innertube/model/media/FormatStreamModel;"
            }
            val arrayListIndex = indexOfFirstInstructionOrThrow(formatStreamModelIndex) {
                opcode == Opcode.INVOKE_INTERFACE &&
                        getReference<MethodReference>()?.toString() == "Ljava/util/List;->add(Ljava/lang/Object;)Z"
            }
            val insertIndex = indexOfFirstInstructionOrThrow(arrayListIndex) {
                opcode == Opcode.INVOKE_INTERFACE &&
                        getReference<MethodReference>()?.toString() == "Ljava/util/List;->isEmpty()Z"
            } + 2

            val formatStreamModelRegister =
                getInstruction<OneRegisterInstruction>(formatStreamModelIndex).registerA
            val arrayListRegister =
                getInstruction<FiveRegisterInstruction>(arrayListIndex).registerC

            addInstructions(
                insertIndex, """
                    invoke-static {v$arrayListRegister}, $GENERAL_CLASS_DESCRIPTOR->getFormatStreamModelArray(Ljava/util/ArrayList;)Ljava/util/ArrayList;
                    move-result-object v$arrayListRegister
                    """
            )

            addInstructions(
                formatStreamModelIndex + 1,
                "invoke-static {v$formatStreamModelRegister}, $GENERAL_CLASS_DESCRIPTOR->setFormatStreamModelArray(Ljava/lang/Object;)V"
            )
        }

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "SETTINGS: DISABLE_AUTO_AUDIO_TRACKS"
            ),
            DISABLE_AUTO_AUDIO_TRACKS
        )

        // endregion

    }
}
