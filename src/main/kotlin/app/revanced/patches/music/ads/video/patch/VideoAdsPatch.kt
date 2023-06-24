package app.revanced.patches.music.ads.video.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.music.utils.litho.patch.LithoFilterPatch
import app.revanced.patches.music.utils.settings.resource.patch.SettingsPatch
import app.revanced.patches.shared.patch.videoads.AbstractVideoAdsPatch
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_ADS_PATH

@Patch
@Name("hide-video-ads")
@Description("Removes ads in the music video.")
@DependsOn(
    [
        LithoFilterPatch::class,
        SettingsPatch::class
    ]
)
@MusicCompatibility
@Version("0.0.1")
class VideoAdsPatch : AbstractVideoAdsPatch(
    "$MUSIC_ADS_PATH/HideMusicAdsPatch;->hideMusicAds()Z"
) {
    override fun execute(context: BytecodeContext): PatchResult {
        super.execute(context)

        SettingsPatch.addMusicPreference(CategoryType.ADS, "revanced_hide_music_ads", "true")

        return PatchResultSuccess()
    }
}
