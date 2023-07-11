package app.revanced.patches.youtube.utils.litho.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.utils.litho.fingerprints.LithoThemeFingerprint
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.instruction.formats.Instruction35c
import org.jf.dexlib2.iface.reference.MethodReference

class LithoThemePatch : BytecodePatch(
    listOf(
        LithoThemeFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        LithoThemeFingerprint.result?.mutableMethod?.let {
            with(it.implementation!!.instructions) {
                for (index in size - 1 downTo 0) {
                    val invokeInstruction = this[index] as? ReferenceInstruction ?: continue
                    if ((invokeInstruction.reference as MethodReference).name != "setColor") continue
                    insertIndex = index
                    insertRegister = (this[index] as Instruction35c).registerD
                    insertMethod = it
                    break
                }
            }
        } ?: return LithoThemeFingerprint.toErrorResult()

        return PatchResultSuccess()
    }

    companion object {
        private var offset = 0

        private var insertIndex: Int = 0
        private var insertRegister: Int = 0
        private lateinit var insertMethod: MutableMethod


        fun injectCall(
            methodDescriptor: String
        ) {
            insertMethod.addInstructions(
                insertIndex + offset, """
                    invoke-static {v$insertRegister}, $methodDescriptor
                    move-result v$insertRegister
                    """
            )
            offset += 2
        }
    }
}

