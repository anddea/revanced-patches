package app.revanced.patches.youtube.video.speed.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object OrganicPlaybackContextModelFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf("Null contentCpn")
)
