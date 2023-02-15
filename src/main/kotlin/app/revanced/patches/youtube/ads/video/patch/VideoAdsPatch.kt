package app.revanced.patches.youtube.ads.video.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.shared.patch.videoads.GeneralVideoAdsPatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.bytecode.BytecodeHelper.updatePatchStatus
import app.revanced.util.integrations.Constants.ADS_PATH

@Patch
@Name("hide-video-ads")
@Description("Removes ads in the video player.")
@DependsOn(
    [
        GeneralVideoAdsPatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class VideoAdsPatch : BytecodePatch() {
    override fun execute(context: BytecodeContext): PatchResult {

        GeneralVideoAdsPatch.injectLegacyAds(INTEGRATIONS_CLASS_DESCRIPTOR)
        GeneralVideoAdsPatch.injectMainstreamAds(INTEGRATIONS_CLASS_DESCRIPTOR)

        context.updatePatchStatus("VideoAds")

        /*
        add settings
        */

        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: ADS_SETTINGS",
                "SETTINGS: HIDE_VIDEO_ADS"
            )
        )

        SettingsPatch.updatePatchStatus("hide-video-ads")

        return PatchResultSuccess()
    }

    private companion object {
        const val INTEGRATIONS_CLASS_DESCRIPTOR = "$ADS_PATH/HideVideoAdsPatch;->hideVideoAds()Z"
    }
}
