package app.revanced.patches.music.general.components.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.music.general.components.fingerprints.SearchBarFingerprint.indexOfVisibilityInstruction
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionReversed
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

object SearchBarFingerprint : MethodFingerprint(
    returnType = "V",
    customFingerprint = { methodDef, _ ->
        indexOfVisibilityInstruction(methodDef) >= 0
    }
) {
    fun indexOfVisibilityInstruction(methodDef: Method) =
        methodDef.indexOfFirstInstructionReversed {
            opcode == Opcode.INVOKE_VIRTUAL &&
                    getReference<MethodReference>()?.name == "setVisibility"
        }
}