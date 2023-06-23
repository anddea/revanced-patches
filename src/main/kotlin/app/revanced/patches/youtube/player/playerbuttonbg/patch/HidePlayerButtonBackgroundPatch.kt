package app.revanced.patches.youtube.player.playerbuttonbg.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.player.playerbuttonbg.fingerprints.HidePlayerButtonFingerprint
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.playerbutton.patch.PlayerButtonPatch
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.INTEGRATIONS_PATH

@Patch
@Name("hide-player-button-background")
@Description("Hide player button background.")
@DependsOn(
    [
        PlayerButtonPatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class HidePlayerButtonBackgroundPatch : BytecodePatch(
    listOf(HidePlayerButtonFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

        HidePlayerButtonFingerprint.result?.mutableMethod?.addInstruction(
            0,
            "invoke-static {p0}, " +
                    "$INTEGRATIONS_PATH/utils/ResourceHelper;->" +
                    "hidePlayerButtonBackground(Landroid/view/View;)V"
        ) ?: return HidePlayerButtonFingerprint.toErrorResult()

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

        return PatchResultSuccess()
    }
}
