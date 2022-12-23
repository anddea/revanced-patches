package app.revanced.patches.music.misc.clientspoof.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.Opcode

object UserAgentHeaderBuilderFingerprint : MethodFingerprint(
    parameters = listOf("L"),
    opcodes = listOf(Opcode.MOVE_RESULT_OBJECT, Opcode.INVOKE_VIRTUAL, Opcode.CONST_16),
    strings = listOf("(Linux; U; Android ")
)