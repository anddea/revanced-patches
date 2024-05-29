package app.revanced.patches.music.video.information.fingerprints

import app.revanced.util.fingerprint.MethodReferenceNameFingerprint
import com.android.tools.smali.dexlib2.Opcode

internal object VideoLengthFingerprint : MethodReferenceNameFingerprint(
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_WIDE,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_WIDE,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_WIDE
    ),
    reference = { "invalidate" }
)