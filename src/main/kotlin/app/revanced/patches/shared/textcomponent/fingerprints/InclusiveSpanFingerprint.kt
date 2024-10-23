package app.revanced.patches.shared.textcomponent.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.shared.textcomponent.fingerprints.InclusiveSpanFingerprint.STARTS_WITH_PARAMETER_LIST
import app.revanced.patches.shared.textcomponent.fingerprints.InclusiveSpanFingerprint.indexOfSetSpanInstruction
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionReversed
import app.revanced.util.parametersEqual
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object InclusiveSpanFingerprint : MethodFingerprint(
    returnType = "V",
    // 19.14 and earlier parameters are:
    // "Landroid/text/SpannableString;",
    // "Ljava/lang/Object;",
    // "I",
    // "I"

    // 19.15+ parameters are:
    // "Landroid/text/SpannableString;",
    // "Ljava/lang/Object;",
    // "I",
    // "I",
    // "Z"
    customFingerprint = custom@{ methodDef, _ ->
        val parameterTypes = methodDef.parameterTypes
        val parameterSize = parameterTypes.size
        if (parameterSize != 4 && parameterSize != 5) {
            return@custom false
        }
        val startsWithMethodParameterList = parameterTypes.slice(0..3)

        if (!parametersEqual(STARTS_WITH_PARAMETER_LIST, startsWithMethodParameterList)) {
            return@custom false
        }
        indexOfSetSpanInstruction(methodDef) >= 0
    },
) {
    internal const val SET_SPAN_METHOD_CALL =
        "Landroid/text/SpannableString;->setSpan(Ljava/lang/Object;III)V"

    private val STARTS_WITH_PARAMETER_LIST = listOf(
        "Landroid/text/SpannableString;",
        "Ljava/lang/Object;",
        "I",
        "I"
    )

    fun indexOfSetSpanInstruction(methodDef: Method) =
        methodDef.indexOfFirstInstructionReversed {
            opcode == Opcode.INVOKE_VIRTUAL &&
                    getReference<MethodReference>().toString() == SET_SPAN_METHOD_CALL
        }
}