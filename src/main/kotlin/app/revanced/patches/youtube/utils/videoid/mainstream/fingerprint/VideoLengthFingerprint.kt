package app.revanced.patches.youtube.utils.videoid.mainstream.fingerprint

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.util.bytecode.isWide32LiteralExists
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode

object VideoLengthFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    opcodes = listOf(
        Opcode.MOVE_RESULT_WIDE,
        Opcode.CONST_4,
        Opcode.INVOKE_VIRTUAL
    ),
    customFingerprint = { it, _ -> it.isWide32LiteralExists(45388753) }
)