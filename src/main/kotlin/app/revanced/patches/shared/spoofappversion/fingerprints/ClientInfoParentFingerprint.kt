package app.revanced.patches.shared.spoofappversion.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

internal object ClientInfoParentFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf("Android Wear")
)
