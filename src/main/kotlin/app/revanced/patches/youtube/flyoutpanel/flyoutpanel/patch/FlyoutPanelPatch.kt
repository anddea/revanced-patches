package app.revanced.patches.youtube.flyoutpanel.flyoutpanel.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.ads.general.resource.patch.GeneralAdsPatch
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch

@Patch
@Name("hide-flyout-panel")
@Description("Adds options to hide player settings flyout panel.")
@DependsOn(
    [
        GeneralAdsPatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class FlyoutPanelPatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

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
