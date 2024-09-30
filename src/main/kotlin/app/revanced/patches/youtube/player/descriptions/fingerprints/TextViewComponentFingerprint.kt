package app.revanced.patches.youtube.player.descriptions.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.player.descriptions.fingerprints.TextViewComponentFingerprint.indexOfTextIsSelectableInstruction
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

/**
 * This fingerprint is compatible with YouTube v18.35.xx~
 * Nonetheless, the patch works in YouTube v19.02.xx~
 */
internal object TextViewComponentFingerprint : MethodFingerprint(
    returnType = "V",
    opcodes = listOf(Opcode.CMPL_FLOAT),
    customFingerprint = { methodDef, _ ->
        methodDef.implementation != null &&
                indexOfTextIsSelectableInstruction(methodDef) >= 0
    },
) {
    fun indexOfTextIsSelectableInstruction(methodDef: Method) =
        methodDef.indexOfFirstInstruction {
            val reference = getReference<MethodReference>()
            opcode == Opcode.INVOKE_VIRTUAL &&
                    reference?.name == "setTextIsSelectable" &&
                    reference.definingClass != "Landroid/widget/TextView;"
        }
}
