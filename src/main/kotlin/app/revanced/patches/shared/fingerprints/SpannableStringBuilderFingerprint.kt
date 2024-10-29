package app.revanced.patches.shared.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.shared.fingerprints.SpannableStringBuilderFingerprint.indexOfSpannableStringInstruction
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object SpannableStringBuilderFingerprint : MethodFingerprint(
    returnType = "Ljava/lang/CharSequence;",
    strings = listOf("Failed to set PB Style Run Extension in TextComponentSpec. Extension id: %s"),
    customFingerprint = { methodDef, _ ->
        indexOfSpannableStringInstruction(methodDef) >= 0
    }
) {
    const val SPANNABLE_STRING_REFERENCE =
        "Landroid/text/SpannableString;->valueOf(Ljava/lang/CharSequence;)Landroid/text/SpannableString;"

    fun indexOfSpannableStringInstruction(methodDef: Method) =
        methodDef.indexOfFirstInstruction {
            opcode == Opcode.INVOKE_STATIC &&
                    getReference<MethodReference>()?.toString() == SPANNABLE_STRING_REFERENCE
        }
}