package app.revanced.patches.youtube.utils.playertype.fingerprint

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.utils.playertype.fingerprint.ActionBarSearchResultsFingerprint.indexOfLayoutDirectionInstruction
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ActionBarSearchResultsViewMic
import app.revanced.util.containsWideLiteralInstructionValue
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object ActionBarSearchResultsFingerprint : MethodFingerprint(
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "Landroid/view/View;",
    customFingerprint = { methodDef, _ ->
        methodDef.containsWideLiteralInstructionValue(ActionBarSearchResultsViewMic) &&
                indexOfLayoutDirectionInstruction(methodDef) >= 0
    }
) {
    fun indexOfLayoutDirectionInstruction(methodDef: Method) =
        methodDef.indexOfFirstInstruction {
            opcode == Opcode.INVOKE_VIRTUAL &&
                    getReference<MethodReference>().toString() == "Landroid/view/View;->setLayoutDirection(I)V"
        }
}