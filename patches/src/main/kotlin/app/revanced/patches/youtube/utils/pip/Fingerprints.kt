package app.revanced.patches.youtube.utils.pip

import app.revanced.patches.youtube.utils.PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR
import app.revanced.util.fingerprint.legacyFingerprint
import com.android.tools.smali.dexlib2.Opcode

internal val pipPlaybackFingerprint = legacyFingerprint(
    name = "pipPlaybackFingerprint",
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
