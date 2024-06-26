package app.revanced.patches.shared.spoofappversion

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patches.shared.fingerprints.CreatePlayerRequestBodyWithModelFingerprint
import app.revanced.patches.shared.fingerprints.CreatePlayerRequestBodyWithModelFingerprint.indexOfReleaseInstruction
import app.revanced.util.getTargetIndexReversedOrThrow
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

abstract class BaseSpoofAppVersionPatch(
    private val descriptor: String
) : BytecodePatch(
    setOf(CreatePlayerRequestBodyWithModelFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        CreatePlayerRequestBodyWithModelFingerprint.resultOrThrow().mutableMethod.apply {
            val versionIndex = indexOfReleaseInstruction(this) + 1
            val insertIndex = getTargetIndexReversedOrThrow(versionIndex, Opcode.IPUT_OBJECT)
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