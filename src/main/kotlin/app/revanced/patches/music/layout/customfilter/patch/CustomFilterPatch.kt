package app.revanced.patches.music.layout.customfilter.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.music.utils.annotations.MusicCompatibility
import app.revanced.patches.music.utils.litho.patch.LithoFilterPatch
import app.revanced.patches.music.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.enum.CategoryType
import app.revanced.util.integrations.Constants.MUSIC_ADS_PATH

@Patch
@Name("Enable custom filter")
@Description("Enables custom filter to hide layout components.")
@DependsOn(
    [
        LithoFilterPatch::class,
        SettingsPatch::class
    ]
)
@MusicCompatibility
@Version("0.0.1")
class CustomFilterPatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        SettingsPatch.addMusicPreference(
            CategoryType.LAYOUT,
            "revanced_custom_filter",
            "false"
        )
        SettingsPatch.addMusicPreferenceWithIntent(
            CategoryType.LAYOUT,
            "revanced_custom_filter_strings",
            "revanced_custom_filter"
        )

        LithoFilterPatch.addFilter(FILTER_CLASS_DESCRIPTOR)

        return PatchResultSuccess()
    }

    private companion object {
        private const val FILTER_CLASS_DESCRIPTOR =
            "$MUSIC_ADS_PATH/CustomFilter;"
    }
}
