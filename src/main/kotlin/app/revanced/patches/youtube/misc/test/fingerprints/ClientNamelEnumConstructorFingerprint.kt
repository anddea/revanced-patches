package app.revanced.patches.youtube.misc.test.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

internal object ClientNamelEnumConstructorFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf(
        "UNKNOWN_INTERFACE",
        "ANDROID_TESTSUITE"
    )
)
