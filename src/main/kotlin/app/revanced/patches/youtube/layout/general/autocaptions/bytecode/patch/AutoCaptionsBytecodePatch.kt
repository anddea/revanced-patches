package app.revanced.patches.youtube.layout.general.autocaptions.bytecode.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.extensions.MethodFingerprintExtensions.name
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultError
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.layout.general.autocaptions.bytecode.fingerprints.*
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.fingerprints.SubtitleButtonControllerFingerprint
import app.revanced.shared.util.integrations.Constants.GENERAL_LAYOUT

@Name("hide-auto-captions-bytecode-patch")
@YouTubeCompatibility
@Version("0.0.1")
class AutoCaptionsBytecodePatch : BytecodePatch(
    listOf(
        StartVideoInformerFingerprint,
        SubtitleButtonControllerFingerprint,
        SubtitleTrackFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {
        listOf(
            StartVideoInformerFingerprint.toPatch(Status.DISABLED),
            SubtitleButtonControllerFingerprint.toPatch(Status.ENABLED)
        ).forEach { (fingerprint, status) ->
            with(fingerprint.result ?: return PatchResultError("Failed to find ${fingerprint.name} method.")) {
                mutableMethod.addInstructions(
                    0,
                    """
                    const/4 v0, ${status.value}
                    sput-boolean v0, $GENERAL_LAYOUT->captionsButtonStatus:Z
                    """
                )
            }
        }

        val subtitleTrackMethod = SubtitleTrackFingerprint.result!!.mutableMethod

        subtitleTrackMethod.addInstructions(
            0, """
            invoke-static {}, $GENERAL_LAYOUT->hideAutoCaptions()Z
            move-result v0
            if-eqz v0, :auto_captions_shown
            sget-boolean v0, $GENERAL_LAYOUT->captionsButtonStatus:Z
            if-nez v0, :auto_captions_shown
            const/4 v0, 0x1
            return v0
            """, listOf(ExternalLabel("auto_captions_shown", subtitleTrackMethod.instruction(0)))
        )

        return PatchResultSuccess()
    }

    private fun MethodFingerprint.toPatch(visibility: Status) = SetStatus(this, visibility)

    private data class SetStatus(val fingerprint: MethodFingerprint, val visibility: Status)

    private enum class Status(val value: Int) {
        ENABLED(1),
        DISABLED(0)
    }
}