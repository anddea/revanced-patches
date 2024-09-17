package app.revanced.patches.youtube.general.toolbar.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.general.toolbar.fingerprints.ActionBarRingoBackgroundFingerprint.indexOfStaticInstruction
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ActionBarRingoBackground
import app.revanced.util.containsWideLiteralInstructionValue
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object ActionBarRingoBackgroundFingerprint : MethodFingerprint(
    returnType = "Landroid/view/View;",
    customFingerprint = { methodDef, _ ->
        methodDef.containsWideLiteralInstructionValue(ActionBarRingoBackground) &&
                indexOfStaticInstruction(methodDef) >= 0
    }
) {
    fun indexOfStaticInstruction(methodDef: Method) =
        methodDef.indexOfFirstInstruction {
            val reference = getReference<MethodReference>()
            opcode == Opcode.INVOKE_STATIC &&
                    reference?.parameterTypes?.size == 1 &&
                    reference.parameterTypes.firstOrNull() == "Landroid/content/Context;" &&
                    reference.returnType == "Z"
        }
}