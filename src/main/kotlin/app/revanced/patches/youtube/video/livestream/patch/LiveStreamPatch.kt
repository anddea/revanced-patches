package app.revanced.patches.youtube.video.livestream.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.video.livestream.fingerprints.OrganicPlaybackContextModelFingerprint
import app.revanced.util.integrations.Constants.VIDEO_PATH

@Name("live-stream-speed")
@YouTubeCompatibility
@Version("0.0.1")
class LiveStreamPatch : BytecodePatch(
    listOf(
        OrganicPlaybackContextModelFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        OrganicPlaybackContextModelFingerprint.result?.mutableMethod?.addInstruction(
            2,
            "invoke-static { p2 }, $INTEGRATIONS_VIDEO_SPEED_CLASS_DESCRIPTOR->liveStreamObserver(Z)V"
        ) ?: return OrganicPlaybackContextModelFingerprint.toErrorResult()

        return PatchResultSuccess()
    }

    private companion object {
        const val INTEGRATIONS_VIDEO_SPEED_CLASS_DESCRIPTOR =
            "$VIDEO_PATH/VideoSpeedPatch;"
    }

}
