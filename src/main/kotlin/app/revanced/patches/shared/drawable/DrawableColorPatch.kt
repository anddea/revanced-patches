package app.revanced.patches.shared.drawable

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.shared.drawable.fingerprints.DrawableFingerprint
import app.revanced.util.getTargetIndexWithMethodReferenceNameReversed
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

object DrawableColorPatch : BytecodePatch(
    setOf(DrawableFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        DrawableFingerprint.resultOrThrow().mutableMethod.apply {
            insertMethod = this
            insertIndex = getTargetIndexWithMethodReferenceNameReversed("setColor")
            insertRegister = getInstruction<FiveRegisterInstruction>(insertIndex).registerD
        }
    }

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

