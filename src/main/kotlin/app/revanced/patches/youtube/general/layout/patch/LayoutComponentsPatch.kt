package app.revanced.patches.youtube.general.layout.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.litho.patch.LithoFilterPatch
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.PATCHES_PATH

@Patch
@Name("hide-layout-components")
@Description("Hides general layout components.")
@DependsOn(
    [
        LithoFilterPatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class LayoutComponentsPatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {
        LithoFilterPatch.addFilter("$PATCHES_PATH/ads/CommunityPostFilter;")
        LithoFilterPatch.addFilter("$PATCHES_PATH/ads/LayoutComponentsFilter;")

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: GENERAL_SETTINGS",
                "PREFERENCE: PLAYER_SETTINGS",

                "SETTINGS: HIDE_AUDIO_TRACK_BUTTON",
                "SETTINGS: HIDE_LAYOUT_COMPONENTS"
            )
        )

        SettingsPatch.updatePatchStatus("hide-layout-components")

        return PatchResultSuccess()
    }
}
