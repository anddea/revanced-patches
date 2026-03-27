package app.morphe.patches.shared.spoof.appversion

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.createPlayerRequestBodyWithModelFingerprint
import app.morphe.patches.shared.indexOfReleaseInstruction
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

fun baseSpoofAppVersionPatch(
    descriptor: String,
) = bytecodePatch(
    description = "baseSpoofAppVersionPatch"
) {
    execute {
        createPlayerRequestBodyWithModelFingerprint.methodOrThrow().apply {
            val versionIndex = indexOfReleaseInstruction(this) + 1
            val insertIndex =
                indexOfFirstInstructionReversedOrThrow(versionIndex, Opcode.IPUT_OBJECT)
            val insertRegister = getInstruction<TwoRegisterInstruction>(insertIndex).registerA

            addInstructions(
                insertIndex, """
                    invoke-static {v$insertRegister}, $descriptor
                    move-result-object v$insertRegister
                    """
            )
        }
    }
}
