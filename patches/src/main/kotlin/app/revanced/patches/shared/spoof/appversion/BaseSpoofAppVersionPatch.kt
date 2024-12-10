package app.revanced.patches.shared.spoof.appversion

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.shared.createPlayerRequestBodyWithModelFingerprint
import app.revanced.patches.shared.indexOfReleaseInstruction
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
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
