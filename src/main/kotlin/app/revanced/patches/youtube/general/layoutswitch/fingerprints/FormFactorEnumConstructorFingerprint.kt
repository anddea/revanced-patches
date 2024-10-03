package app.revanced.patches.youtube.general.layoutswitch.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

internal object FormFactorEnumConstructorFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf(
        "UNKNOWN_FORM_FACTOR",
        "SMALL_FORM_FACTOR",
        "LARGE_FORM_FACTOR"
    )
)
