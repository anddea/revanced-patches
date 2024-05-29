package app.revanced.patches.youtube.utils.fix.formatstream.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.utils.fix.formatstream.fingerprints.EndpointUrlBuilderFingerprint.indexOfToStringInstruction
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object EndpointUrlBuilderFingerprint : MethodFingerprint(
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL or AccessFlags.DECLARED_SYNCHRONIZED,
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_VIRTUAL,    // Uri.toString()
        Opcode.MOVE_RESULT_OBJECT,
    ),
    strings = listOf("asig"),
    customFingerprint = { methodDef, _ ->
        indexOfToStringInstruction(methodDef) >= 0
    }
) {
   fun indexOfToStringInstruction(methodDef: Method) =
        methodDef.indexOfFirstInstruction {
            opcode == Opcode.INVOKE_VIRTUAL
                    && getReference<MethodReference>()?.definingClass == "Landroid/net/Uri;"
                    && getReference<MethodReference>()?.name == "toString"
        }
}

