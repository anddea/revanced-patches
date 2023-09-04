package app.revanced.patches.youtube.player.collapsebutton.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.playerbutton.patch.PlayerButtonHookPatch
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch

@Patch
@Name("Hide collapse button")
@Description("Hides the collapse button in the video player.")
@DependsOn(
    [
        PlayerButtonHookPatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
class HideCollapseButtonPatch : ResourcePatch {
    override fun execute(context: ResourceContext) {

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: PLAYER_SETTINGS",
                "SETTINGS: HIDE_COLLAPSE_BUTTON"
            )
        )

        SettingsPatch.updatePatchStatus("hide-collapse-button")

    }
}
