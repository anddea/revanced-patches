package app.revanced.patches.youtube.player.musicbutton.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.player.musicbutton.fingerprints.MusicAppDeeplinkButtonFingerprint
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.PLAYER

@Patch
@Name("Hide music button")
@Description("Hides the YouTube Music button in the video player.")
@DependsOn(
    [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@YouTubeCompatibility
class HideMusicButtonPatch : BytecodePatch(
    listOf(MusicAppDeeplinkButtonFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        MusicAppDeeplinkButtonFingerprint.result?.let {
            it.mutableMethod.apply {
                addInstructionsWithLabels(
                    0,
                    """
                        invoke-static {}, $PLAYER->hideMusicButton()Z
                        move-result v0
                        if-nez v0, :hidden
                        """,
                    ExternalLabel("hidden", getInstruction(implementation!!.instructions.size - 1))
                )
            }
        } ?: throw MusicAppDeeplinkButtonFingerprint.exception

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: PLAYER_SETTINGS",
                "SETTINGS: HIDE_YOUTUBE_MUSIC_BUTTON"
            )
        )

        SettingsPatch.updatePatchStatus("hide-music-button")

    }
}
