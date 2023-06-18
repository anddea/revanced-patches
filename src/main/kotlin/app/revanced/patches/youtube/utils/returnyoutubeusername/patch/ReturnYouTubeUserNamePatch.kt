package app.revanced.patches.youtube.utils.returnyoutubeusername.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.utils.returnyoutubeusername.fingerprints.SpannableStringBuilderFingerprint
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.instruction.formats.Instruction35c

@Patch
@Name("return-youtube-user-name")
@Description("Replace user handles in YouTube comments with user nicknames.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class ReturnYouTubeUserNamePatch : BytecodePatch(
    listOf(SpannableStringBuilderFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

        SpannableStringBuilderFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.startIndex
                val targetReference = getInstruction<ReferenceInstruction>(targetIndex).reference
                val targetRegister = getInstruction<Instruction35c>(targetIndex).registerC

                addInstructions(
                    targetIndex + 1, """
                        invoke-static {v$targetRegister}, Lapp/revanced/integrations/patches/utils/ReturnYouTubeCommentUsernamePatch;->onCharSequenceLoaded(Ljava/lang/CharSequence;)Ljava/lang/CharSequence;
                        move-result-object v$targetRegister                        
                        invoke-static {v$targetRegister}, $targetReference
                        """
                )
                removeInstruction(targetIndex)
            }
        } ?: return SpannableStringBuilderFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
}
