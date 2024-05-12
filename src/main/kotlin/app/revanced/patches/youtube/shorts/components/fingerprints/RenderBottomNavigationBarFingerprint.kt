package app.revanced.patches.youtube.shorts.components.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.Opcode

internal object RenderBottomNavigationBarFingerprint : MethodFingerprint(
    returnType = "V",
    opcodes = listOf(
        Opcode.CONST_STRING,
        Opcode.INVOKE_VIRTUAL,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.IF_NEZ
    ),
    strings = listOf("r_ipl")
)