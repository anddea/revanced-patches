package app.revanced.patches.youtube.video.restrictions.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object VideoCapabilitiesParentFingerprint : MethodFingerprint(
    returnType = "L",
    strings = listOf("minh.", ";maxh.")
)
