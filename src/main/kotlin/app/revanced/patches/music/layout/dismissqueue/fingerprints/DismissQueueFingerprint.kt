package app.revanced.patches.music.layout.dismissqueue.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.util.bytecode.isWide32LiteralExists
import com.android.tools.smali.dexlib2.Opcode

object DismissQueueFingerprint : MethodFingerprint(
    returnType = "V",
    opcodes = listOf(
        Opcode.CONST_WIDE_32,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT
    ),
    customFingerprint = { methodDef, _ -> methodDef.isWide32LiteralExists(45413042) }
)