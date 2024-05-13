package app.revanced.patches.reddit.layout.screenshotpopup

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.reddit.layout.screenshotpopup.fingerprints.ScreenshotTakenBannerFingerprint
import app.revanced.patches.reddit.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.reddit.utils.integrations.Constants.PATCHES_PATH
import app.revanced.patches.reddit.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.reddit.utils.settings.SettingsBytecodePatch.updateSettingsStatus
import app.revanced.patches.reddit.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow

@Suppress("unused")
object ScreenshotPopupPatch : BaseBytecodePatch(
    name = "Disable screenshot popup",
    description = "Adds an option to disable the popup that shows up when taking a screenshot.",
    dependencies = setOf(
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(ScreenshotTakenBannerFingerprint)
) {
    private const val INTEGRATIONS_METHOD_DESCRIPTOR =
        "$PATCHES_PATH/ScreenshotPopupPatch;->disableScreenshotPopup()Z"

    override fun execute(context: BytecodeContext) {

        ScreenshotTakenBannerFingerprint.resultOrThrow().let {
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
        }

        updateSettingsStatus("enableScreenshotPopup")

    }
}
