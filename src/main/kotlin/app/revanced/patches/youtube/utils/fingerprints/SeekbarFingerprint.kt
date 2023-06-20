package app.revanced.patches.youtube.utils.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object SeekbarFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf("timed_markers_width")
)