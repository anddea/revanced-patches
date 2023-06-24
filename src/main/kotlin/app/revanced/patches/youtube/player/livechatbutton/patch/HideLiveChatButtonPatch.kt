package app.revanced.patches.youtube.player.livechatbutton.patch

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
import app.revanced.patches.youtube.utils.playerbutton.patch.PlayerButtonHookPatch
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch

@Patch
@Name("hide-live-chat-button")
@Description("Hides the live chat button in the video player (for old layout).")
@DependsOn(
    [
        PlayerButtonHookPatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class HideLiveChatButtonPatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: PLAYER_SETTINGS",
                "SETTINGS: HIDE_LIVE_CHATS_BUTTON"
            )
        )

        SettingsPatch.updatePatchStatus("hide-live-chat-button")

        return PatchResultSuccess()
    }
}