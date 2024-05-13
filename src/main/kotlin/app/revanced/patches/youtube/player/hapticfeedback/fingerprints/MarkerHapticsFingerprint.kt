package app.revanced.patches.youtube.player.hapticfeedback.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

internal object MarkerHapticsFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf("Failed to execute markers haptics vibrate.")
)