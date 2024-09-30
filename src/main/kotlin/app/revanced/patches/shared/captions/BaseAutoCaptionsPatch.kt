package app.revanced.patches.shared.captions

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.captions.fingerprints.StoryboardRendererDecoderRecommendedLevelFingerprint
import app.revanced.patches.shared.captions.fingerprints.SubtitleTrackFingerprint
import app.revanced.patches.shared.fingerprints.StartVideoInformerFingerprint
import app.revanced.patches.shared.integrations.Constants.PATCHES_PATH
import app.revanced.util.resultOrThrow

@Patch(
    description = "Disable forced auto captions for YouTube or YouTube Music."
)
object BaseAutoCaptionsPatch : BytecodePatch(
    setOf(
        StartVideoInformerFingerprint,
        StoryboardRendererDecoderRecommendedLevelFingerprint,
        SubtitleTrackFingerprint,
    )
) {
    private const val INTEGRATIONS_CLASS_DESCRIPTOR =
        "$PATCHES_PATH/AutoCaptionsPatch;"

    override fun execute(context: BytecodeContext) {

        SubtitleTrackFingerprint.resultOrThrow().mutableMethod.apply {
            addInstructionsWithLabels(
                0, """
                    invoke-static {}, $INTEGRATIONS_CLASS_DESCRIPTOR->disableAutoCaptions()Z
                    move-result v0
                    if-eqz v0, :disabled
                    const/4 v0, 0x1
                    return v0
                    """, ExternalLabel("disabled", getInstruction(0))
            )
        }

        mapOf(
            StartVideoInformerFingerprint to 0,
            StoryboardRendererDecoderRecommendedLevelFingerprint to 1
        ).forEach { (fingerprint, enabled) ->
            fingerprint.resultOrThrow().mutableMethod.addInstructions(
                0, """
                    const/4 v0, 0x$enabled
                    invoke-static {v0}, $INTEGRATIONS_CLASS_DESCRIPTOR->setCaptionsButtonStatus(Z)V
                    """
            )
        }

    }
}