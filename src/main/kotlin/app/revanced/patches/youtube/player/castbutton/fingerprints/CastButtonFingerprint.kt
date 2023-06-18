package app.revanced.patches.youtube.player.castbutton.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object CastButtonFingerprint : MethodFingerprint(
    customFingerprint = { it, _ -> it.definingClass.endsWith("MediaRouteButton;") && it.name == "setVisibility" }
)