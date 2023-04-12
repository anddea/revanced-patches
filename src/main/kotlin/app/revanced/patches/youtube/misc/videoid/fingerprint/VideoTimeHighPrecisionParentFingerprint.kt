package app.revanced.patches.youtube.misc.videoid.fingerprint

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object VideoTimeHighPrecisionParentFingerprint : MethodFingerprint(
    strings = listOf("MedialibPlayerTimeInfo{currentPositionMillis=")
)