package app.revanced.patches.reddit.layout.screenshotpopup

import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.reddit.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.reddit.utils.extension.Constants.PATCHES_PATH
import app.revanced.patches.reddit.utils.patch.PatchList.DISABLE_SCREENSHOT_POPUP
import app.revanced.patches.reddit.utils.resourceid.screenShotShareBanner
import app.revanced.patches.reddit.utils.resourceid.sharedResourceIdPatch
import app.revanced.patches.reddit.utils.settings.is_2025_06_or_greater
import app.revanced.patches.reddit.utils.settings.settingsPatch
import app.revanced.patches.reddit.utils.settings.updatePatchStatus
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import app.revanced.util.indexOfFirstLiteralInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_METHOD_DESCRIPTOR =
    "$PATCHES_PATH/ScreenshotPopupPatch;->disableScreenshotPopup()Z"

@Suppress("unused")
val screenshotPopupPatch = bytecodePatch(
    DISABLE_SCREENSHOT_POPUP.title,
    DISABLE_SCREENSHOT_POPUP.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        sharedResourceIdPatch,
    )

    execute {

        if (is_2025_06_or_greater) {
            screenshotTakenBannerFingerprint.methodOrThrow().apply {
                val literalIndex = indexOfFirstLiteralInstructionOrThrow(screenShotShareBanner)
                val insertIndex = indexOfFirstInstructionReversedOrThrow(literalIndex, Opcode.CONST_4)
                val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA
                val jumpIndex = indexOfFirstInstructionOrThrow(literalIndex, Opcode.SGET_OBJECT)

                addInstructionsWithLabels(
                    insertIndex, """
                        invoke-static {}, $EXTENSION_METHOD_DESCRIPTOR
                        move-result v$insertRegister
                        if-nez v$insertRegister, :hidden
                        """, ExternalLabel("hidden", getInstruction(jumpIndex))
                )
            }
        } else {
            screenshotTakenBannerLegacyFingerprint.methodOrThrow().apply {
                addInstructionsWithLabels(
                    0, """
                        invoke-static {}, $EXTENSION_METHOD_DESCRIPTOR
                        move-result v0
                        if-eqz v0, :dismiss
                        return-void
                        """, ExternalLabel("dismiss", getInstruction(0))
                )
            }
        }

        updatePatchStatus(
            "enableScreenshotPopup",
            DISABLE_SCREENSHOT_POPUP
        )
    }
}
