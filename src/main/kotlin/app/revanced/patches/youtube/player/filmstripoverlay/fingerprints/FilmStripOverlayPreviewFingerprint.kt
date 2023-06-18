package app.revanced.patches.youtube.player.filmstripoverlay.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.Opcode

object FilmStripOverlayPreviewFingerprint : MethodFingerprint(
    returnType = "Z",
    parameters = listOf("F"),
    opcodes = listOf(
        Opcode.SUB_FLOAT,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT
    )
)