package app.revanced.patches.shared.fingerprints.customspeed

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

object SpeedLimiterFallBackFingerprint : MethodFingerprint(
    returnType = "F",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.CONST_HIGH16,
        Opcode.CONST_HIGH16,
        Opcode.INVOKE_STATIC
    ),
    strings = listOf("Playback rate: %f")
)
