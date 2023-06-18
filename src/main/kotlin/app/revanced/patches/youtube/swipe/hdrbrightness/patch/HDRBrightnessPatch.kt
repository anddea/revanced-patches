package app.revanced.patches.youtube.swipe.hdrbrightness.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.swipe.hdrbrightness.fingerprints.HDRBrightnessFingerprint
import app.revanced.util.integrations.Constants.SWIPE_PATH

@Name("disable-hdr-auto-brightness")
@YouTubeCompatibility
@Version("0.0.1")
class HDRBrightnessPatch : BytecodePatch(
    listOf(HDRBrightnessFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

        HDRBrightnessFingerprint.result?.let {
            it.mutableMethod.apply {
                addInstructionsWithLabels(
                    0, """
                        invoke-static {}, $SWIPE_PATH/DisableHDRAutoBrightnessPatch;->disableHDRAutoBrightness()Z
                        move-result v0
                        if-eqz v0, :default
                        return-void
                        """, ExternalLabel("default", getInstruction(0))
                )
            }
        } ?: return HDRBrightnessFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
}