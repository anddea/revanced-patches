package app.morphe.patches.shared.spoof.guide

import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

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

