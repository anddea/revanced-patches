package app.revanced.patches.youtube.player.musicbutton.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object MusicAppDeeplinkButtonFingerprint : MethodFingerprint(
    returnType = "V",
    parameters = listOf("Z", "Z"),
    customFingerprint = { it, _ -> it.definingClass.endsWith("MusicAppDeeplinkButtonController;") }
)