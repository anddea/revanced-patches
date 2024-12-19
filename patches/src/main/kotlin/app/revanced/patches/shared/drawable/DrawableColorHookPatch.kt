package app.revanced.patches.shared.drawable

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private lateinit var insertMethod: MutableMethod
private var insertIndex: Int = 0
private var insertRegister: Int = 0
private var offset = 0

val drawableColorHookPatch = bytecodePatch(
    description = "drawableColorHookPatch"
) {
    execute {
        drawableColorFingerprint.methodOrThrow().apply {
            insertMethod = this
            insertIndex = indexOfFirstInstructionReversedOrThrow {
                getReference<MethodReference>()?.name == "setColor"
            }
            insertRegister = getInstruction<FiveRegisterInstruction>(insertIndex).registerD
        }
    }
}

internal fun addDrawableColorHook(
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

