package app.revanced.patches.youtube.video.playback.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

internal object DeviceDimensionsModelToStringFingerprint : MethodFingerprint(
    returnType = "L",
    strings = listOf("minh.", ";maxh.")
)
