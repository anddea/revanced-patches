package app.revanced.patches.youtube.misc.videoid.mainstream.fingerprint

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object VideoTimeParentFingerprint : MethodFingerprint(
    strings = listOf("MedialibPlayerTimeInfo{currentPositionMillis=")
)