package app.revanced.patches.music.ads.music

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.music.utils.litho.LithoFilterPatch
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.patches.shared.patch.ads.AbstractAdsPatch
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_ADS_PATH
import app.revanced.util.integrations.Constants.MUSIC_COMPONENTS_PATH

@Patch(
    name = "Hide music ads",
    description = "Hides ads before playing a music.",
    dependencies = [
        LithoFilterPatch::class,
        SettingsPatch::class
    ],
    compatiblePackages = [CompatiblePackage("com.google.android.apps.youtube.music")]
)
@Suppress("unused")
object MusicAdsPatch : AbstractAdsPatch(
    "$MUSIC_ADS_PATH/MusicAdsPatch;->hideMusicAds()Z"
) {
    override fun execute(context: BytecodeContext) {
        super.execute(context)

        LithoFilterPatch.addFilter(FILTER_CLASS_DESCRIPTOR)

        SettingsPatch.addMusicPreference(CategoryType.ADS, "revanced_close_interstitial_ads", "false")
        SettingsPatch.addMusicPreference(CategoryType.ADS, "revanced_hide_music_ads", "true")
    }

    private const val FILTER_CLASS_DESCRIPTOR =
        "$MUSIC_COMPONENTS_PATH/AdsFilter;"
}
