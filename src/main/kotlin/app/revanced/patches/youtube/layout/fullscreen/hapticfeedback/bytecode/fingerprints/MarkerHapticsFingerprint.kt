package app.revanced.patches.youtube.layout.fullscreen.hapticfeedback.bytecode.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object MarkerHapticsFingerprint : MethodFingerprint(
    strings = listOf("Failed to execute markers haptics vibrate.")
)