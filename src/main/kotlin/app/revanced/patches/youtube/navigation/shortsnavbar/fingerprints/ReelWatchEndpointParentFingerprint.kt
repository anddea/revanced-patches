package app.revanced.patches.youtube.navigation.shortsnavbar.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object ReelWatchEndpointParentFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf("Error parsing bytes for updated ReelWatchEndpoint.", "r_aoc")
)