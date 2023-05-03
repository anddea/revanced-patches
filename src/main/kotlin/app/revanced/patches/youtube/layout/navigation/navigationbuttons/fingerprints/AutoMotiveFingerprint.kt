package app.revanced.patches.youtube.layout.navigation.navigationbuttons.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.Opcode

object AutoMotiveFingerprint : MethodFingerprint(
    opcodes = listOf(
        Opcode.GOTO,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT,
        Opcode.IF_EQZ
    ),
    strings = listOf("Android Automotive")
)