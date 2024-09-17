package app.revanced.patches.shared.drawable

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.shared.drawable.fingerprints.DrawableFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

object DrawableColorPatch : BytecodePatch(
    setOf(DrawableFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        DrawableFingerprint.resultOrThrow().mutableMethod.apply {
            insertMethod = this
            insertIndex = indexOfFirstInstructionReversedOrThrow {
                getReference<MethodReference>()?.name == "setColor"
            }
            insertRegister = getInstruction<FiveRegisterInstruction>(insertIndex).registerD
        }
    }

    private lateinit var insertMethod: MutableMethod
    private var insertIndex: Int = 0
    private var insertRegister: Int = 0
    private var offset = 0

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

