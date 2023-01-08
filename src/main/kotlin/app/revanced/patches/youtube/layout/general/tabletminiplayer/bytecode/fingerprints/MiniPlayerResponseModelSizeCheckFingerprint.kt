package app.revanced.patches.youtube.layout.general.tabletminiplayer.bytecode.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode

object MiniPlayerResponseModelSizeCheckFingerprint : MethodFingerprint(
    returnType = "L",
    access = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L", "L"),
    opcodes = listOf(
        Opcode.RETURN_OBJECT,
        Opcode.CHECK_CAST,
        Opcode.CHECK_CAST,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT,
        Opcode.IF_NEZ
    )
)