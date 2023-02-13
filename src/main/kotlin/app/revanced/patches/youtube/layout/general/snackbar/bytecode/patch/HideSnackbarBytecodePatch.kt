package app.revanced.patches.youtube.layout.general.snackbar.bytecode.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.layout.general.snackbar.bytecode.fingerprints.HideSnackbarFingerprint
import app.revanced.util.integrations.Constants.GENERAL_LAYOUT

@Name("hide-snackbar-bytecode-patch")
@YouTubeCompatibility
@Version("0.0.1")
class HideSnackbarBytecodePatch : BytecodePatch(
    listOf(
        HideSnackbarFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        HideSnackbarFingerprint.result?.mutableMethod?.let {
            it.addInstructions(
                0, """
                    invoke-static {}, $GENERAL_LAYOUT->hideSnackbar()Z
                    move-result v0
                    if-eqz v0, :default
                    return-void
                    """, listOf(ExternalLabel("default", it.instruction(0)))
            )
        } ?: return HideSnackbarFingerprint.toErrorResult()

        return PatchResultSuccess()
    }
}
