package app.revanced.patches.shared.tracking.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.Opcode

/**
 * Copy URL from sharing panel
 */
internal object CopyTextEndpointFingerprint : MethodFingerprint(
    returnType = "V",
    parameters = listOf("L", "Ljava/util/Map;"),
    opcodes = listOf(
        Opcode.IGET_OBJECT,
        Opcode.CONST_STRING,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.IGET_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_INTERFACE,
        Opcode.RETURN_VOID
    ),
    strings = listOf("text/plain")
)