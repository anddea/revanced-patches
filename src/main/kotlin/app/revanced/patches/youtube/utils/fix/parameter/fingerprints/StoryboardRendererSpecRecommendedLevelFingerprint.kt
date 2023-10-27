package app.revanced.patches.youtube.utils.fix.parameter.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.Opcode

object StoryboardRendererSpecRecommendedLevelFingerprint : MethodFingerprint(
    strings = listOf("#-1#"),
    opcodes = listOf(
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IPUT_OBJECT,
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT
    )
)
