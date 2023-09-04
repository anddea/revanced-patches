package app.revanced.patches.youtube.player.castbutton.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.youtube.player.castbutton.fingerprints.CastButtonFingerprint
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.PLAYER

@Patch
@Name("Hide cast button")
@Description("Hides the cast button in the video player.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
class HideCastButtonPatch : BytecodePatch(
    listOf(CastButtonFingerprint)
) {
    override fun execute(context: BytecodeContext) {
        CastButtonFingerprint.result?.let {
            it.mutableMethod.apply {
                addInstructions(
                    0, """
                        invoke-static {p1}, $PLAYER->hideCastButton(I)I
                        move-result p1
                        """
                )
            }
        } ?: throw CastButtonFingerprint.exception

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: PLAYER_SETTINGS",
                "SETTINGS: HIDE_CAST_BUTTON"
            )
        )

        SettingsPatch.updatePatchStatus("hide-cast-button")

    }
}
