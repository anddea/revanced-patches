package app.revanced.patches.youtube.flyoutpanel.player.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.litho.patch.LithoFilterPatch
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.PATCHES_PATH

@Patch
@Name("Hide player flyout panel")
@Description("Hides player flyout panel components.")
@DependsOn(
    [
        LithoFilterPatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
class PlayerFlyoutPanelPatch : ResourcePatch {
    override fun execute(context: ResourceContext) {
        LithoFilterPatch.addFilter("$PATCHES_PATH/ads/PlayerFlyoutPanelsFilter;")

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: FLYOUT_PANEL_SETTINGS",
                "SETTINGS: PLAYER_FLYOUT_PANEL_HEADER",
                "SETTINGS: HIDE_PLAYER_FLYOUT_PANEL"
            )
        )

        SettingsPatch.updatePatchStatus("hide-player-flyout-panel")

    }
}
