package app.revanced.patches.youtube.layout.navigation.shortsnavbar.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object ReelWatchEndpointParentFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf("Error parsing bytes for updated ReelWatchEndpoint.", "r_aoc")
)