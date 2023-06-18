package app.revanced.patches.youtube.swipe.hdrbrightness.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object HDRBrightnessFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf("mediaViewambientBrightnessSensor")
)
