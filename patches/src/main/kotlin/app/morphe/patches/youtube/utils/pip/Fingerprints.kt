package app.morphe.patches.youtube.utils.pip

import app.morphe.patches.youtube.utils.PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR
import app.morphe.util.fingerprint.legacyFingerprint
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

internal val pipPlaybackModernFingerprint = legacyFingerprint(
    name = "pipPlaybackModernFingerprint",
    returnType = "Lcom/google/common/util/concurrent/ListenableFuture;",
    parameters = listOf("Landroid/view/View;"),
    strings = listOf("Error entering picture and picture"),
    opcodes = listOf(
        Opcode.INVOKE_DIRECT,
        Opcode.MOVE_RESULT,
        Opcode.IF_NEZ
    )
)
