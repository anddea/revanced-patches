package app.revanced.patches.reddit.layout.screenshotpopup.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.reddit.layout.screenshotpopup.fingerprints.ScreenshotTakenBannerFingerprint
import app.revanced.patches.reddit.utils.annotations.RedditCompatibility
import app.revanced.patches.reddit.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.reddit.utils.settings.bytecode.patch.SettingsBytecodePatch.Companion.updateSettingsStatus
import app.revanced.patches.reddit.utils.settings.resource.patch.SettingsPatch

@Patch
@Name("Disable screenshot popup")
@Description("Disables the popup that shows up when taking a screenshot.")
@DependsOn(
    [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@RedditCompatibility
class ScreenshotPopupPatch : BytecodePatch(
    listOf(ScreenshotTakenBannerFingerprint)
) {
    override fun execute(context: BytecodeContext) {

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
        } ?: throw ScreenshotTakenBannerFingerprint.exception

        updateSettingsStatus("ScreenshotPopup")

    }

    private companion object {
        private const val INTEGRATIONS_METHOD_DESCRIPTOR =
            "Lapp/revanced/reddit/patches/ScreenshotPopupPatch;" +
                    "->disableScreenshotPopup()Z"
    }
}
