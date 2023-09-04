package app.revanced.patches.youtube.player.previousnextbutton.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.playerbutton.patch.PlayerButtonHookPatch
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch

@Patch
@Name("Hide previous next button")
@Description("Hides the previous and next button in the player controller.")
@DependsOn(
    [
        PlayerButtonHookPatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
class HidePreviousNextButtonPatch : BytecodePatch() {
    override fun execute(context: BytecodeContext) {

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: PLAYER_SETTINGS",
                "SETTINGS: HIDE_PREVIOUS_NEXT_BUTTON"
            )
        )

        SettingsPatch.updatePatchStatus("hide-previous-next-button")

    }
}
