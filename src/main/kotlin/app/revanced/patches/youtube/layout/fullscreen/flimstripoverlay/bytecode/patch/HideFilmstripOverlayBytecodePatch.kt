package app.revanced.patches.youtube.layout.fullscreen.flimstripoverlay.bytecode.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.removeInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patches.youtube.layout.fullscreen.flimstripoverlay.bytecode.fingerprints.ScrubbingLabelFingerprint
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.util.integrations.Constants.FULLSCREEN_LAYOUT
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.instruction.TwoRegisterInstruction
import org.jf.dexlib2.iface.reference.FieldReference
import org.jf.dexlib2.Opcode

@Name("hide-filmstrip-overlay-bytecode-patch")
@YouTubeCompatibility
@Version("0.0.1")
class HideFilmstripOverlayBytecodePatch : BytecodePatch(
    listOf(
        ScrubbingLabelFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {
        val scrubbingLabelResult = ScrubbingLabelFingerprint.result!!
        val scrubbingLabelMethod = scrubbingLabelResult.mutableMethod
        val scrubbingLabelMethodInstructions = scrubbingLabelMethod.implementation!!.instructions

        for ((index, instruction) in scrubbingLabelMethodInstructions.withIndex()) {
            if (instruction.opcode != Opcode.IPUT_BOOLEAN) continue
            val scrubbingLabelRegisterA = (instruction as TwoRegisterInstruction).registerA
            val scrubbingLabelRegisterB = scrubbingLabelRegisterA + 2
            val scrubbingLabelRegisterC = (instruction as TwoRegisterInstruction).registerB
            val scrubbingLabelReference = (instruction as ReferenceInstruction).reference as FieldReference

            scrubbingLabelMethod.addInstructions(
                index + 1, """
                    invoke-static {}, $FULLSCREEN_LAYOUT->hideFilmstripOverlay()Z
                    move-result v$scrubbingLabelRegisterB
                    if-eqz v$scrubbingLabelRegisterB, :show
                    const/4 v$scrubbingLabelRegisterA, 0x0
                    :show
                    iput-boolean v$scrubbingLabelRegisterA, v$scrubbingLabelRegisterC, ${scrubbingLabelReference.definingClass}->${scrubbingLabelReference.name}:${scrubbingLabelReference.type}
                    """
            )

            scrubbingLabelMethod.removeInstruction(index)
            break
        }

        return PatchResultSuccess()
    }
}
