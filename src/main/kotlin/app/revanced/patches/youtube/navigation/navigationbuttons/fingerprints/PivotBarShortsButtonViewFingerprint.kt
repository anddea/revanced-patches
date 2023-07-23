package app.revanced.patches.youtube.navigation.navigationbuttons.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import com.android.tools.smali.dexlib2.Opcode

object PivotBarShortsButtonViewFingerprint : MethodFingerprint(
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL_RANGE,
        Opcode.MOVE_RESULT_OBJECT, // target reference
        Opcode.GOTO,
    )
)