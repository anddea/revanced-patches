package app.revanced.patches.youtube.player.musicbutton

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.player.musicbutton.fingerprints.MusicAppDeeplinkButtonFingerprint
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.integrations.Constants.PLAYER

@Patch(
    name = "Hide music button",
    description = "Hides the YouTube Music button in the video player.",
    dependencies = [
        SettingsPatch::class,
        SharedResourceIdPatch::class,
    ],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.youtube",
            [
                "18.24.37",
                "18.25.40",
                "18.27.36",
                "18.29.38",
                "18.30.37",
                "18.31.40",
                "18.32.39",
                "18.33.40",
                "18.34.38",
                "18.35.36",
                "18.36.39",
                "18.37.36"
            ]
        )
    ]
)
@Suppress("unused")
object HideMusicButtonPatch : BytecodePatch(
    setOf(MusicAppDeeplinkButtonFingerprint)
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
