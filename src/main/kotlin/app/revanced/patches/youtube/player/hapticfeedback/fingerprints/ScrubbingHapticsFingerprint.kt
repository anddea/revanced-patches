package app.revanced.patches.youtube.player.hapticfeedback.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

internal object ScrubbingHapticsFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf("Failed to haptics vibrate for fine scrubbing.")
)
