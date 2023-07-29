package app.revanced.patches.youtube.buttomplayer.buttoncontainer.patch

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
@Name("Hide button container")
@Description("Adds the options to hide action buttons under a video.")
@DependsOn(
    [
        LithoFilterPatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class ButtonContainerPatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        if (SettingsPatch.below1820) {
            LithoFilterPatch.addFilter("$PATCHES_PATH/ads/ActionButtonsFilter;")
            SettingsPatch.addPreference(
                arrayOf(
                    "PREFERENCE: BOTTOM_PLAYER_SETTINGS",
                    "SETTINGS: EXPERIMENTAL_BUTTON_CONTAINER"
                )
            )
        }

        LithoFilterPatch.addFilter("$PATCHES_PATH/ads/ButtonsFilter;")

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: BOTTOM_PLAYER_SETTINGS",
                "SETTINGS: BUTTON_CONTAINER"
            )
        )

        SettingsPatch.updatePatchStatus("hide-button-container")

        return PatchResultSuccess()
    }
}
