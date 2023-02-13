package app.revanced.patches.youtube.ads.video.bytecode.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.shared.patch.videoads.GeneralVideoAdsPatch
import app.revanced.util.bytecode.BytecodeHelper
import app.revanced.util.integrations.Constants.ADS_PATH

@DependsOn(
    [
        GeneralVideoAdsPatch::class
    ]
)
@Name("hide-video-ads-bytecode-patch")
@YouTubeCompatibility
@Version("0.0.1")
class VideoAdsBytecodePatch : BytecodePatch() {
    override fun execute(context: BytecodeContext): PatchResult {

        GeneralVideoAdsPatch.injectLegacyAds(INTEGRATIONS_CLASS_DESCRIPTOR)
        GeneralVideoAdsPatch.injectMainstreamAds(INTEGRATIONS_CLASS_DESCRIPTOR)

        BytecodeHelper.patchStatus(context, "VideoAds")

        return PatchResultSuccess()
    }

    private companion object {
        const val INTEGRATIONS_CLASS_DESCRIPTOR = "$ADS_PATH/HideVideoAdsPatch;->hideVideoAds()Z"
    }
}
