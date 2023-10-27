package app.revanced.patches.youtube.fullscreen.landscapemode.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

object OrientationParentFingerprint : MethodFingerprint(
    returnType = "Z",
    strings = listOf("NoClassDefFoundError thrown while verifying stack trace.")
)