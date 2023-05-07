package app.revanced.patches.youtube.layout.general.snackbar.patch

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
import app.revanced.patches.youtube.layout.general.snackbar.fingerprints.HideSnackBarFingerprint
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.GENERAL

@Patch
@Name("hide-snack-bar")
@Description("Hides the snack bar action popup.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class HideSnackBarPatch : BytecodePatch(
    listOf(
        HideSnackBarFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        HideSnackBarFingerprint.result?.mutableMethod?.let {
            it.addInstructions(
                0, """
                    invoke-static {}, $GENERAL->hideSnackBar()Z
                    move-result v0
                    if-eqz v0, :default
                    return-void
                    """, listOf(ExternalLabel("default", it.instruction(0)))
            )
        } ?: return HideSnackBarFingerprint.toErrorResult()

        /*
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: GENERAL_SETTINGS",
                "SETTINGS: HIDE_SNACK_BAR"
            )
        )

        SettingsPatch.updatePatchStatus("hide-snack-bar")

        return PatchResultSuccess()
    }
}
