package app.revanced.patches.shared.captions

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.captions.fingerprints.SubtitleButtonControllerFingerprint
import app.revanced.patches.shared.captions.fingerprints.SubtitleTrackFingerprint
import app.revanced.patches.shared.fingerprints.StartVideoInformerFingerprint
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

abstract class BaseAutoCaptionsPatch(
    private val classDescriptor: String,
    private val captionsButtonStatus: Boolean
) : BytecodePatch(
    setOf(
        StartVideoInformerFingerprint,
        SubtitleButtonControllerFingerprint,
        SubtitleTrackFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        SubtitleTrackFingerprint.resultOrThrow().mutableMethod.apply {
            if (captionsButtonStatus) {
                addInstructionsWithLabels(
                    0, """
                        invoke-static {}, $classDescriptor->disableAutoCaptions()Z
                        move-result v0
                        if-eqz v0, :disabled
                        const/4 v0, 0x1
                        return v0
                        """, ExternalLabel("disabled", getInstruction(0))
                )
            } else {
                val index = implementation!!.instructions.lastIndex
                val register = getInstruction<OneRegisterInstruction>(index).registerA

                addInstructions(
                    index, """
                        invoke-static {v$register}, $classDescriptor->disableAutoCaptions(Z)Z
                        move-result v$register
                        """
                )
            }
        }

        if (!captionsButtonStatus) return

        mapOf(
            StartVideoInformerFingerprint to 0,
            SubtitleButtonControllerFingerprint to 1
        ).forEach { (fingerprint, enabled) ->
            fingerprint.resultOrThrow().mutableMethod.addInstructions(
                0, """
                    const/4 v0, 0x$enabled
                    invoke-static {v0}, $classDescriptor->setCaptionsButtonStatus(Z)V
                    """
            )
        }
    }
}