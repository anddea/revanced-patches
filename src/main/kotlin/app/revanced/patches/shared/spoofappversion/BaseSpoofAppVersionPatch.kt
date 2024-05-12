package app.revanced.patches.shared.spoofappversion

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patches.shared.spoofappversion.fingerprints.ClientInfoFingerprint
import app.revanced.patches.shared.spoofappversion.fingerprints.ClientInfoParentFingerprint
import app.revanced.util.getTargetIndexReversed
import app.revanced.util.getTargetIndexWithFieldReferenceName
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

abstract class BaseSpoofAppVersionPatch(
    private val descriptor: String
) : BytecodePatch(
    setOf(ClientInfoParentFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        ClientInfoParentFingerprint.resultOrThrow().let { parentResult ->
            ClientInfoFingerprint.resolve(context, parentResult.classDef)

            ClientInfoFingerprint.resultOrThrow().let {
                it.mutableMethod.apply {
                    val versionIndex = getTargetIndexWithFieldReferenceName("RELEASE") + 1
                    val insertIndex = getTargetIndexReversed(versionIndex, Opcode.IPUT_OBJECT)
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

    }
}