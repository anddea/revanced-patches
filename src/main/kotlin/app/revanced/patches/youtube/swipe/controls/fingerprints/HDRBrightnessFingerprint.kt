package app.revanced.patches.youtube.swipe.controls.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object HDRBrightnessFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf("mediaViewambientBrightnessSensor")
)
