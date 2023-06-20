package app.revanced.patches.youtube.player.playerbuttonbg.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object HidePlayerButtonFingerprint : MethodFingerprint(
    customFingerprint = { it, _ -> it.definingClass.endsWith("PlayerPatch;") && it.name == "hidePlayerButton" }
)