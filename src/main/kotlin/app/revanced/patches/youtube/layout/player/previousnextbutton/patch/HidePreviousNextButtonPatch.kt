package app.revanced.patches.youtube.layout.player.previousnextbutton.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.shared.fingerprints.ControlsOverlayStyleFingerprint
import app.revanced.patches.youtube.layout.player.previousnextbutton.fingerprints.SupportsNextPreviousFingerprint
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.PLAYER

@Patch
@Name("hide-previous-next-button")
@Description("Hides the previous and next button in the player controller.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class HidePreviousNextButtonPatch : BytecodePatch(
    listOf(ControlsOverlayStyleFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

        ControlsOverlayStyleFingerprint.result?.let { parentResult ->
            SupportsNextPreviousFingerprint.also { it.resolve(context, parentResult.classDef) }.result?.mutableMethod?.addInstructions(
                0, """
                    invoke-static {p1}, $PLAYER->hidePreviousNextButton(Z)Z
                    move-result p1
                    """
            ) ?: return SupportsNextPreviousFingerprint.toErrorResult()
        } ?: return ControlsOverlayStyleFingerprint.toErrorResult()

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
