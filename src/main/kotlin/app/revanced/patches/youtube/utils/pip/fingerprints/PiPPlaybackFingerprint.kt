package app.revanced.patches.youtube.utils.pip.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.utils.PlayerResponseModelUtils.PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR
import com.android.tools.smali.dexlib2.Opcode

internal object PiPPlaybackFingerprint : MethodFingerprint(
    returnType = "Z",
    parameters = listOf(PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR),
    opcodes = listOf(
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT,
        Opcode.IF_NEZ
    )
)