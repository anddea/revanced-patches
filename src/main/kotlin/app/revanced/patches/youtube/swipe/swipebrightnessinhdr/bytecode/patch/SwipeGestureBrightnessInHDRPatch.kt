package app.revanced.patches.youtube.swipe.swipebrightnessinhdr.bytecode.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.swipe.swipebrightnessinhdr.bytecode.fingerprints.HDRVideoFingerprint
import app.revanced.util.integrations.Constants.SWIPE_PATH

@Name("enable-swipe-gesture-brightness-in-hdr-patch")
@YouTubeCompatibility
@Version("0.0.1")
class SwipeGestureBrightnessInHDRPatch : BytecodePatch(
    listOf(
        HDRVideoFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        HDRVideoFingerprint.result?.mutableMethod?.let {
            it.addInstructions(
                0, """
                    invoke-static {}, $SWIPE_PATH/EnableSwipeGestureBrightnessInHDRPatch;->enableSwipeGestureBrightnessInHDR()Z
                    move-result v0
                    if-eqz v0, :default
                    return-void
                """, listOf(ExternalLabel("default", it.instruction(0)))
            )
        } ?: return HDRVideoFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
}