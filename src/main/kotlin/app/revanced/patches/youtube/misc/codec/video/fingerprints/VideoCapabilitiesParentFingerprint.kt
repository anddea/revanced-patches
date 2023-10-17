package app.revanced.patches.youtube.misc.codec.video.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object VideoCapabilitiesParentFingerprint : MethodFingerprint(
    returnType = "L",
    strings = listOf("minh.", ";maxh.")
)
