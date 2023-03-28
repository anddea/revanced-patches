package app.revanced.patches.youtube.layout.general.accountmenu.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.Opcode

object LibraryMenuFingerprint : MethodFingerprint(
    returnType = "V",
    parameters = listOf("L", "L"),
    opcodes = listOf(
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IGET_OBJECT
    )
)