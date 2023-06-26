package app.revanced.patches.youtube.utils.videoid.general.fingerprint

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object VideoTimeHighPrecisionParentFingerprint : MethodFingerprint(
    strings = listOf("MedialibPlayerTimeInfo{currentPositionMillis=")
)