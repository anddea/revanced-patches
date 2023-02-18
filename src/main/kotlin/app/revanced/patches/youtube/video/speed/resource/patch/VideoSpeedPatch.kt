package app.revanced.patches.youtube.video.speed.resource.patch

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
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.patches.youtube.video.speed.bytecode.patch.VideoSpeedBytecodePatch

@Patch
@Name("default-video-speed")
@Description("Adds ability to set default video speed settings.")
@DependsOn(
    [
        VideoSpeedBytecodePatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class VideoSpeedPatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        /*
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: VIDEO_SETTINGS",
                "SETTINGS: DEFAULT_VIDEO_SPEED"
            )
        )

        SettingsPatch.updatePatchStatus("default-video-speed")

        return PatchResultSuccess()
    }
}