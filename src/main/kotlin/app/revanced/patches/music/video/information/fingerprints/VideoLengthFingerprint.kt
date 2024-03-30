package app.revanced.patches.music.video.information.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.Opcode

object VideoLengthFingerprint : MethodFingerprint(
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_WIDE,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_WIDE,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_WIDE,
        null, // Opcode.CONST_4 or Opcode.INVOKE_VIRTUAL
        null, // Opcode.INVOKE_VIRTUAL or Opcode.MOVE_RESULT
        null, // Opcode.MOVE_RESULT or Opcode.CONST_4
        Opcode.IF_EQ
    )
)