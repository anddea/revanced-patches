package app.revanced.patches.youtube.fullscreen.endscreenoverlay.patch

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
import app.revanced.patches.youtube.fullscreen.endscreenoverlay.fingerprints.EndScreenResultsFingerprint
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.FULLSCREEN

@Patch
@Name("Hide end screen overlay")
@Description("Hide end screen overlay on swipe controls.")
@DependsOn(
    [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@YouTubeCompatibility
class HideEndScreenOverlayPatch : BytecodePatch(
    listOf(EndScreenResultsFingerprint)
) {
    override fun execute(context: BytecodeContext) {
        EndScreenResultsFingerprint.result?.let {
            it.mutableMethod.apply {
                addInstructionsWithLabels(
                    0, """
                        invoke-static {}, $FULLSCREEN->hideEndScreenOverlay()Z
                        move-result v0
                        if-eqz v0, :show
                        return-void
                """, ExternalLabel("show", getInstruction(0))
                )
            }
        } ?: throw EndScreenResultsFingerprint.exception

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: FULLSCREEN_SETTINGS",
                "SETTINGS: HIDE_END_SCREEN_OVERLAY"
            )
        )

        SettingsPatch.updatePatchStatus("hide-endscreen-overlay")

    }
}
