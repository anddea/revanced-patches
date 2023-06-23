package app.revanced.patches.youtube.utils.videoid.mainstream.fingerprint

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object MainstreamVideoIdParentFingerprint : MethodFingerprint(
    returnType = "V",
    parameters = listOf("L", "L"),
    strings = listOf("error retrieving subtitle"),
)
