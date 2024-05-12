package app.revanced.patches.youtube.player.fullscreen.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

internal object OrientationParentFingerprint : MethodFingerprint(
    returnType = "Z",
    strings = listOf("NoClassDefFoundError thrown while verifying stack trace.")
)