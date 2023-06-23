package app.revanced.patches.youtube.player.castbutton.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object CastButtonFingerprint : MethodFingerprint(
    customFingerprint = { methodDef, _ -> methodDef.definingClass.endsWith("MediaRouteButton;") && methodDef.name == "setVisibility" }
)