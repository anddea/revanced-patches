package app.revanced.patches.youtube.overlaybutton.downloadbuttonhook.patch

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.overlaybutton.downloadbuttonhook.fingerprints.DownloadActionsFingerprint
import app.revanced.util.integrations.Constants.UTILS_PATH

class DownloadButtonHookPatch : BytecodePatch(
    listOf(DownloadActionsFingerprint)
) {
    override fun execute(context: BytecodeContext) {
        DownloadActionsFingerprint.result?.let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.startIndex

                addInstructionsWithLabels(
                    targetIndex, """
                        invoke-static {}, $UTILS_PATH/HookDownloadButtonPatch;->shouldHookDownloadButton()Z
                        move-result v0
                        if-eqz v0, :default
                        invoke-static {}, $UTILS_PATH/HookDownloadButtonPatch;->startDownloadActivity()V
                        return-void
                        """, ExternalLabel("default", getInstruction(targetIndex))
                )
            }
        } ?: throw DownloadActionsFingerprint.exception

    }
}
