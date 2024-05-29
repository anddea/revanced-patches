package app.revanced.patches.youtube.player.components.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.Opcode

internal object FilmStripOverlayPreviewFingerprint : MethodFingerprint(
    returnType = "Z",
    parameters = listOf("F"),
    opcodes = listOf(
        Opcode.SUB_FLOAT,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT
    )
)