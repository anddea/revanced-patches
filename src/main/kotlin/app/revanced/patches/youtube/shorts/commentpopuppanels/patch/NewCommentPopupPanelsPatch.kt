package app.revanced.patches.youtube.shorts.commentpopuppanels.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.shorts.commentpopuppanels.fingerprints.ReelWatchFragmentBuilderFingerprint
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.bytecode.getWide32LiteralIndex
import app.revanced.util.integrations.Constants.SHORTS
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction

@Patch
@Name("enable-new-comment-popup-panels")
@Description("Enables a new type of comment popup panel in the shorts player.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class NewCommentPopupPanelsPatch : BytecodePatch(
    listOf(ReelWatchFragmentBuilderFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

        ReelWatchFragmentBuilderFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = getWide32LiteralIndex(45401415) + 2
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstructions(
                    targetIndex + 1, """
                        invoke-static {}, $SHORTS->enableNewCommentPopupPanels()Z
                        move-result v$targetRegister
                        """
                )
            }
        } ?: return ReelWatchFragmentBuilderFingerprint.toErrorResult()

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "SETTINGS: ENABLE_NEW_COMMENT_POPUP_PANELS"
            )
        )

        SettingsPatch.updatePatchStatus("enable-new-comment-popup-panels")

        return PatchResultSuccess()
    }
}
