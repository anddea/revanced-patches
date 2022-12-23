package app.revanced.patches.youtube.video.quality.resource.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patches.youtube.video.quality.bytecode.patch.VideoQualityBytecodePatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.util.resources.ResourceHelper
import app.revanced.shared.util.resources.ResourceUtils.copyXmlNode

@Patch
@Name("default-video-quality")
@Description("Adds ability to set default video quality settings.")
@DependsOn(
    [
        VideoQualityBytecodePatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class VideoQualityPatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        /*
         * Copy arrays
         */

        context.copyXmlNode("youtube/quality/host", "values/arrays.xml", "resources")


        /*
         add settings
         */
        ResourceHelper.addSettings(
            context,
            "PREFERENCE_CATEGORY: REVANCED_EXTENDED_SETTINGS",
            "PREFERENCE: VIDEO_SETTINGS",
            "SETTINGS: DEFAULT_VIDEO_QUALITY"
        )

        ResourceHelper.patchSuccess(
            context,
            "default-video-quality"
        )

        return PatchResultSuccess()
    }
}