package app.revanced.patches.youtube.navigation.homepage.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.Opcode

object IntentExceptionFingerprint : MethodFingerprint(
    returnType = "V",
    parameters = listOf("L", "L"),
    opcodes = listOf(Opcode.MOVE_EXCEPTION),
    strings = listOf("Failed to resolve intent")
)