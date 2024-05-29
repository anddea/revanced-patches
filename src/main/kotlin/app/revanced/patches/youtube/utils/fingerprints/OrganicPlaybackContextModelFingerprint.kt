package app.revanced.patches.youtube.utils.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

internal object OrganicPlaybackContextModelFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf("Null contentCpn")
)
