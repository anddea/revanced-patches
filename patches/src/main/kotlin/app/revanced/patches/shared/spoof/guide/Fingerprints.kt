package app.revanced.patches.shared.spoof.guide

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal val authenticationChangeListenerFingerprint = legacyFingerprint(
    name = "authenticationChangeListenerFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.FINAL,
    strings = listOf("Authentication changed while request was being made"),
    customFingerprint = { method, _ ->
        indexOfMessageLiteBuilderReference(method) >= 0
    }
)

internal fun indexOfMessageLiteBuilderReference(method: Method, type: String = "L") =
    method.indexOfFirstInstruction {
        val reference = getReference<MethodReference>()
        opcode == Opcode.INVOKE_VIRTUAL &&
                reference?.parameterTypes?.isEmpty() == true &&
                reference.returnType.startsWith(type)
    }

internal val guideEndpointConstructorFingerprint = legacyFingerprint(
    name = "guideEndpointConstructorFingerprint",
    returnType = "V",
    strings = listOf("guide"),
    customFingerprint = { method, _ ->
        method.name == "<init>"
    }
)

internal val guideEndpointRequestBodyFingerprint = legacyFingerprint(
    name = "guideEndpointRequestBodyFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PROTECTED or AccessFlags.FINAL,
    parameters = emptyList(),
    opcodes = listOf(Opcode.RETURN_VOID),
)

