package app.revanced.patches.youtube.player.filmstripoverlay.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.util.bytecode.isWide32LiteralExists

object FilmStripOverlayConfigFingerprint : MethodFingerprint(
    returnType = "Z",
    parameters = listOf(),
    customFingerprint = { it, _ -> it.isWide32LiteralExists(45381958) }
)