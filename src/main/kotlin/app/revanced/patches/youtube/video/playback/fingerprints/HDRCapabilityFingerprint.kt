package app.revanced.patches.youtube.video.playback.fingerprints

import app.revanced.util.fingerprint.MethodReferenceNameFingerprint

internal object HDRCapabilityFingerprint : MethodReferenceNameFingerprint(
    returnType = "Z",
    parameters = listOf("I", "Landroid/view/Display;"),
    reference = { "getSupportedHdrTypes" }
)
