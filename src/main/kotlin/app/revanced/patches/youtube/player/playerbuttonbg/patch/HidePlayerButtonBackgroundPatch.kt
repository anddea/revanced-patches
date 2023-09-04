package app.revanced.patches.youtube.player.playerbuttonbg.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.player.playerbuttonbg.fingerprints.PlayerPatchFingerprint
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.playerbutton.patch.PlayerButtonHookPatch
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.INTEGRATIONS_PATH

@Patch
@Name("Hide player button background")
@Description("Hide player button background.")
@DependsOn(
    [
        PlayerButtonHookPatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
class HidePlayerButtonBackgroundPatch : BytecodePatch(
    listOf(PlayerPatchFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        PlayerPatchFingerprint.result?.mutableMethod?.addInstruction(
            0,
            "invoke-static {p0}, " +
                    "$INTEGRATIONS_PATH/utils/ResourceHelper;->" +
                    "hidePlayerButtonBackground(Landroid/view/View;)V"
        ) ?: throw PlayerPatchFingerprint.exception

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: PLAYER_SETTINGS",
                "SETTINGS: HIDE_PLAYER_BUTTON_BACKGROUND"
            )
        )

        SettingsPatch.updatePatchStatus("hide-player-button-background")

    }
}
