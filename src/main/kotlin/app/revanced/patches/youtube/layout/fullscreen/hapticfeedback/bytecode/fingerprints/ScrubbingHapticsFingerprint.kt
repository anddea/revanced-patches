package app.revanced.patches.youtube.layout.fullscreen.hapticfeedback.bytecode.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object ScrubbingHapticsFingerprint : MethodFingerprint(
    strings = listOf("Failed to haptics vibrate for fine scrubbing.")
)
