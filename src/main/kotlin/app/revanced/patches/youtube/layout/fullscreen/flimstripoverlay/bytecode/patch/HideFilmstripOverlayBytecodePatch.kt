package app.revanced.patches.youtube.layout.fullscreen.flimstripoverlay.bytecode.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.removeInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultError
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.layout.fullscreen.flimstripoverlay.bytecode.fingerprints.ScrubbingLabelFingerprint
import app.revanced.util.integrations.Constants.FULLSCREEN_LAYOUT
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.instruction.TwoRegisterInstruction
import org.jf.dexlib2.iface.reference.FieldReference

@Name("hide-filmstrip-overlay-bytecode-patch")
@YouTubeCompatibility
@Version("0.0.1")
class HideFilmstripOverlayBytecodePatch : BytecodePatch(
    listOf(
        ScrubbingLabelFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {
        ScrubbingLabelFingerprint.result?.mutableMethod?.let {
            with (it.implementation!!.instructions) {
                for ((index, instruction) in this.withIndex()) {
                    if (instruction.opcode != Opcode.IPUT_BOOLEAN) continue
                    val primaryRegister = (instruction as TwoRegisterInstruction).registerA
                    val secondaryRegister = (instruction as TwoRegisterInstruction).registerB
                    val dummyRegister = primaryRegister + 2
                    val fieldReference = (instruction as ReferenceInstruction).reference as FieldReference

                    it.addInstructions(
                        index + 1, """
                            invoke-static {}, $FULLSCREEN_LAYOUT->hideFilmstripOverlay()Z
                            move-result v$dummyRegister
                            if-eqz v$dummyRegister, :show
                            const/4 v$primaryRegister, 0x0
                            :show
                            iput-boolean v$primaryRegister, v$secondaryRegister, ${fieldReference.definingClass}->${fieldReference.name}:${fieldReference.type}
                        """
                    )

                    it.removeInstruction(index)

                    return PatchResultSuccess()
                }
            }
        } ?: return ScrubbingLabelFingerprint.toErrorResult()

        return PatchResultError("Could not find the method to hook.")
    }
}
