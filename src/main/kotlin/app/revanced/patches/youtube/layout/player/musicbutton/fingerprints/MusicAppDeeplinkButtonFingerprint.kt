package app.revanced.patches.youtube.layout.player.musicbutton.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object MusicAppDeeplinkButtonFingerprint : MethodFingerprint(
    returnType = "V",
    parameters = listOf("Z", "Z"),
    customFingerprint = { it.definingClass.endsWith("MusicAppDeeplinkButtonController;") }
)