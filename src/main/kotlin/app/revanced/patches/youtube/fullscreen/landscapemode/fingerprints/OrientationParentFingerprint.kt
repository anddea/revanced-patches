package app.revanced.patches.youtube.fullscreen.landscapemode.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object OrientationParentFingerprint : MethodFingerprint(
    returnType = "Z",
    strings = listOf("NoClassDefFoundError thrown while verifying stack trace.")
)