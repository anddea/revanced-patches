package app.revanced.patches.youtube.utils.sponsorblock.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.utils.sponsorblock.fingerprints.RectangleFieldInvalidatorFingerprint.indexOfInvalidateInstruction
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionReversed
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object RectangleFieldInvalidatorFingerprint : MethodFingerprint(
    returnType = "V",
    parameters = emptyList(),
    customFingerprint = { methodDef, _ ->
        indexOfInvalidateInstruction(methodDef) >= 0
    }
) {
    fun indexOfInvalidateInstruction(methodDef: Method) =
        methodDef.indexOfFirstInstructionReversed {
            getReference<MethodReference>()?.name == "invalidate"
        }
}
