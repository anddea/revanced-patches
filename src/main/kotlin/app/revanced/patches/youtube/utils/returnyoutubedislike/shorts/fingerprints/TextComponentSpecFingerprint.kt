package app.revanced.patches.youtube.utils.returnyoutubedislike.shorts.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.Opcode

object TextComponentSpecFingerprint : MethodFingerprint(
    returnType = "Ljava/lang/CharSequence;",
    opcodes = listOf(
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
        null, // Opcode.CONST_4 or Opcode.MOVE
        Opcode.INVOKE_INTERFACE_RANGE
    ),
    strings = listOf("Failed to set PB Style Run Extension in TextComponentSpec. Extension id: %s")
)