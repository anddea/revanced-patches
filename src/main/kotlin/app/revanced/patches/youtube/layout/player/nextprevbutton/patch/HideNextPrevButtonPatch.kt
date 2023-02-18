package app.revanced.patches.youtube.layout.player.nextprevbutton.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.misc.playerbutton.patch.PlayerButtonPatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch

@Patch
@Name("hide-next-prev-button")
@Description("Hides the next prev button in the player controller.")
@DependsOn(
    [
        PlayerButtonPatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class HideNextPrevButtonPatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        /*
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: LAYOUT_SETTINGS",
                "PREFERENCE_HEADER: PLAYER",
                "SETTINGS: HIDE_NEXT_BUTTON",
                "SETTINGS: HIDE_PREV_BUTTON"
            )
        )

        SettingsPatch.updatePatchStatus("hide-next-prev-button")

        return PatchResultSuccess()
    }
}
