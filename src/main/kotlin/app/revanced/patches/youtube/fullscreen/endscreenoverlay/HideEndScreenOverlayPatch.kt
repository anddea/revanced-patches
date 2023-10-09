package app.revanced.patches.youtube.fullscreen.endscreenoverlay

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.fullscreen.endscreenoverlay.fingerprints.EndScreenResultsFingerprint
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.integrations.Constants.FULLSCREEN

@Patch(
    name = "Hide end screen overlay",
    description = "Hide end screen overlay on swipe controls.",
    dependencies = [
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.youtube",
            [
                "18.22.37",
                "18.23.36",
                "18.24.37",
                "18.25.40",
                "18.27.36",
                "18.29.38",
                "18.30.37",
                "18.31.40",
                "18.32.39",
                "18.33.40"
            ]
        )
    ]
)
@Suppress("unused")
object HideEndScreenOverlayPatch : BytecodePatch(
    setOf(EndScreenResultsFingerprint)
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
