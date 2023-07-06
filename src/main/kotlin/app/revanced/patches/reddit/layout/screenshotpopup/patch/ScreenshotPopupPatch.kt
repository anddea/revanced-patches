package app.revanced.patches.reddit.layout.screenshotpopup.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.reddit.layout.screenshotpopup.fingerprints.ScreenshotTakenBannerFingerprint
import app.revanced.patches.reddit.utils.annotations.RedditCompatibility
import app.revanced.patches.reddit.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.reddit.utils.settings.bytecode.patch.SettingsBytecodePatch.Companion.updateSettingsStatus
import app.revanced.patches.reddit.utils.settings.resource.patch.SettingsPatch

@Patch
@Name("disable-screenshot-popup")
@Description("Disables the popup that shows up when taking a screenshot.")
@DependsOn(
    [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@RedditCompatibility
@Version("0.0.1")
class ScreenshotPopupPatch : BytecodePatch(
    listOf(ScreenshotTakenBannerFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

        ScreenshotTakenBannerFingerprint.result?.let {
            it.mutableMethod.apply {
                addInstructionsWithLabels(
                    0, """
                        invoke-static {}, $INTEGRATIONS_METHOD_DESCRIPTOR
                        move-result v0
                        if-eqz v0, :dismiss
                        return-void
                        """, ExternalLabel("dismiss", getInstruction(0))
                )
            }
        } ?: return ScreenshotTakenBannerFingerprint.toErrorResult()

        updateSettingsStatus("ScreenshotPopup")

        return PatchResultSuccess()
    }

    private companion object {
        private const val INTEGRATIONS_METHOD_DESCRIPTOR =
            "Lapp/revanced/reddit/patches/ScreenshotPopupPatch;" +
                    "->disableScreenshotPopup()Z"
    }
}
