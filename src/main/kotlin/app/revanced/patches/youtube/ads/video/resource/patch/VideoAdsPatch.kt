package app.revanced.patches.youtube.ads.video.resource.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.ads.video.bytecode.patch.VideoAdsBytecodePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.resources.ResourceHelper

@Patch
@Name("hide-video-ads")
@Description("Removes ads in the video player.")
@DependsOn(
    [
        VideoAdsBytecodePatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class VideoAdsPatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        /*
         add settings
         */
        ResourceHelper.addSettings(
            context,
            "PREFERENCE_CATEGORY: REVANCED_SETTINGS",
            "PREFERENCE: ADS_SETTINGS",
            "SETTINGS: HIDE_VIDEO_ADS"
        )

        ResourceHelper.patchSuccess(
            context,
            "hide-video-ads"
        )

        return PatchResultSuccess()
    }
}