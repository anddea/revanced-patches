package app.revanced.patches.youtube.layout.fullscreen.endscreenoverlay.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.layout.fullscreen.endscreenoverlay.fingerprints.EndScreenResultsFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.FULLSCREEN

@Patch
@Name("hide-endscreen-overlay")
@Description("Hide endscreen overlay on swipe controls.")
@DependsOn(
    [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class HideEndScreenOverlayPatch : BytecodePatch(
    listOf(EndScreenResultsFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {
        EndScreenResultsFingerprint.result?.mutableMethod?.let {
            it.addInstructions(
                0, """
                invoke-static {}, $FULLSCREEN->hideEndScreenOverlay()Z
                move-result v0
                if-eqz v0, :show
                return-void
                """, listOf(ExternalLabel("show", it.instruction(0)))
            )
        } ?: return EndScreenResultsFingerprint.toErrorResult()

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

        return PatchResultSuccess()
    }
}
