package app.revanced.patches.youtube.layout.fullscreen.hapticfeedback.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object ZoomHapticsFingerprint : MethodFingerprint(
    strings = listOf("Failed to haptics vibrate for video zoom")
)