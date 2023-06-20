package app.revanced.patches.youtube.ads.video.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.patch.videoads.AbstractVideoAdsPatch
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.ADS_PATH

@Patch
@Name("hide-video-ads")
@Description("Removes ads in the video player.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class VideoAdsPatch : AbstractVideoAdsPatch(
    "$ADS_PATH/HideVideoAdsPatch;->hideVideoAds()Z"
) {
    override fun execute(context: BytecodeContext): PatchResult {
        super.execute(context)

        /**
         * Add settings
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
}
