package app.revanced.patches.youtube.player.previousnextbutton.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.utils.playerbutton.patch.PlayerButtonPatch
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch

@Patch
@Name("hide-previous-next-button")
@Description("Hides the previous and next button in the player controller.")
@DependsOn(
    [
        PlayerButtonPatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class HidePreviousNextButtonPatch : BytecodePatch() {
    override fun execute(context: BytecodeContext): PatchResult {

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

        return PatchResultSuccess()
    }
}
