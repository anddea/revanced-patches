package app.revanced.patches.youtube.utils.videoid.general.fingerprint

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object VideoIdParentFingerprint : MethodFingerprint(
    returnType = "V",
    parameters = listOf("L", "L"),
    strings = listOf("error retrieving subtitle"),
)
