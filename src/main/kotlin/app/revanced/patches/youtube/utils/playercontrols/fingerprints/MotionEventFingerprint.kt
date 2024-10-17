package app.revanced.patches.youtube.utils.playercontrols.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.utils.playercontrols.fingerprints.MotionEventFingerprint.indexOfTranslationInstruction
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionReversed
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object MotionEventFingerprint : MethodFingerprint(
    returnType = "V",
    parameters = listOf("Landroid/view/MotionEvent;"),
    customFingerprint = { methodDef, _ ->
        indexOfTranslationInstruction(methodDef) >= 0
    }
) {
    fun indexOfTranslationInstruction(methodDef: Method) =
        methodDef.indexOfFirstInstructionReversed {
            getReference<MethodReference>()?.name == "setTranslationY"
        }
}
