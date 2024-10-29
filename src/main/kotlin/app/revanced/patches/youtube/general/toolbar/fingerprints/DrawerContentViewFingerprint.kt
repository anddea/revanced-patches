package app.revanced.patches.youtube.general.toolbar.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.general.toolbar.fingerprints.DrawerContentViewFingerprint.indexOfAddViewInstruction
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionReversed
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object DrawerContentViewFingerprint : MethodFingerprint(
    returnType = "V",
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.NEW_INSTANCE,
        Opcode.INVOKE_DIRECT,
    ),
    customFingerprint = { methodDef, _ ->
        indexOfAddViewInstruction(methodDef) >= 0
    }
) {
    fun indexOfAddViewInstruction(methodDef: Method) =
        methodDef.indexOfFirstInstructionReversed {
            opcode == Opcode.INVOKE_VIRTUAL &&
                    getReference<MethodReference>()?.name == "addView"
        }
}
