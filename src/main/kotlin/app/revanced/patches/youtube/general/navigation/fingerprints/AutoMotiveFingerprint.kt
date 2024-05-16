package app.revanced.patches.youtube.general.navigation.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.Opcode

internal object AutoMotiveFingerprint : MethodFingerprint(
    opcodes = listOf(
        Opcode.GOTO,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT,
        Opcode.IF_EQZ
    ),
    strings = listOf("Android Automotive")
)