package app.revanced.patches.youtube.shorts.shortsnavigationbar.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import com.android.tools.smali.dexlib2.Opcode

object BottomNavigationBarFingerprint : MethodFingerprint(
    returnType = "V",
    opcodes = listOf(
        Opcode.IGET_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IF_EQZ
    ),
    strings = listOf("r_pfvc", "navigation_endpoint_interaction_logging_extension")
)