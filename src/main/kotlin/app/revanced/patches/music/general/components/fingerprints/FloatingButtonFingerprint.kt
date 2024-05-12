package app.revanced.patches.music.general.components.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.Opcode

internal object FloatingButtonFingerprint : MethodFingerprint(
    returnType = "V",
    parameters = listOf("L"),
    opcodes = listOf(Opcode.AND_INT_LIT16)
)

