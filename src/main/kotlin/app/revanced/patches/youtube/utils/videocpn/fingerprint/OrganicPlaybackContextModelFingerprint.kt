package app.revanced.patches.youtube.utils.videocpn.fingerprint

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object OrganicPlaybackContextModelFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf("Null contentCpn")
)
