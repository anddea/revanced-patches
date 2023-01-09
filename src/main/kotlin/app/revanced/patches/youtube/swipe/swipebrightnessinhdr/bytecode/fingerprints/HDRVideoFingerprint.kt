package app.revanced.patches.youtube.swipe.swipebrightnessinhdr.bytecode.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object HDRVideoFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf("mediaViewambientBrightnessSensor")
)
