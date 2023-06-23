package app.revanced.patches.youtube.player.playerbuttonbg.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object HidePlayerButtonFingerprint : MethodFingerprint(
    customFingerprint = { methodDef, _ -> methodDef.definingClass.endsWith("PlayerPatch;") && methodDef.name == "hidePlayerButton" }
)