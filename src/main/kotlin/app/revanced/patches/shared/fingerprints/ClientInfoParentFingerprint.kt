package app.revanced.patches.shared.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object ClientInfoParentFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf("Android Wear")
)
