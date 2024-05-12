package app.revanced.patches.youtube.general.audiotracks

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patches.youtube.general.audiotracks.fingerprints.StreamingModelBuilderFingerprint
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.getTargetIndexWithReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

@Suppress("unused")
object AudioTracksPatch : BaseBytecodePatch(
    name = "Disable auto audio tracks",
    description = "Adds an option to disable audio tracks from being automatically enabled.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(StreamingModelBuilderFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        StreamingModelBuilderFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val formatStreamModelIndex = indexOfFirstInstruction {
                    opcode == Opcode.CHECK_CAST
                            && (this as ReferenceInstruction).reference.toString() == "Lcom/google/android/libraries/youtube/innertube/model/media/FormatStreamModel;"
                }
                val arrayListIndex = getTargetIndexWithReference(formatStreamModelIndex, "Ljava/util/List;->add(Ljava/lang/Object;)Z")
                val insertIndex = getTargetIndexWithReference(arrayListIndex, "Ljava/util/List;->isEmpty()Z") + 2

                val formatStreamModelRegister = getInstruction<OneRegisterInstruction>(formatStreamModelIndex).registerA
                val arrayListRegister = getInstruction<FiveRegisterInstruction>(arrayListIndex).registerC

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
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "SETTINGS: DISABLE_AUTO_AUDIO_TRACKS"
            )
        )

        SettingsPatch.updatePatchStatus(this)

    }
}
