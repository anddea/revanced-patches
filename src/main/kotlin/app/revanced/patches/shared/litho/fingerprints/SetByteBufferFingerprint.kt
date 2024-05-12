package app.revanced.patches.shared.litho.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object SetByteBufferFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("I", "Ljava/nio/ByteBuffer;"),
    opcodes = listOf(
        null,
        Opcode.IF_EQZ,
        Opcode.IPUT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.SUB_INT_2ADDR,
        Opcode.IPUT,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.IPUT,
        Opcode.RETURN_VOID,
        Opcode.CONST_4,
        Opcode.IPUT,
        Opcode.IPUT,
        Opcode.GOTO
    ),
    // Check method count and field count to support both YouTube and YouTube Music
    customFingerprint = { _, classDef ->
        classDef.methods.count() > 6
                && classDef.fields.count() > 4
    }
)