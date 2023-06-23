package app.revanced.patches.youtube.flyoutpanel.general.patch

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
@Name("hide-flyout-panel")
@Description("Adds options to hide player settings flyout panel.")
@DependsOn(
    [
        LithoFilterPatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class FlyoutPanelPatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {
        LithoFilterPatch.addFilter("$PATCHES_PATH/ads/FlyoutPanelsFilter;")

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: FLYOUT_PANEL_SETTINGS",
                "SETTINGS: FLYOUT_PANEL_COMPONENT"
            )
        )

        SettingsPatch.updatePatchStatus("hide-flyout-panel")

        return PatchResultSuccess()
    }
}
