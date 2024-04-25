package app.revanced.patches.youtube.navigation.navigationbuttons.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

internal const val ANDROID_AUTOMOTIVE_STRING = "Android Automotive"

internal object AddCreateButtonViewFingerprint : MethodFingerprint(
    strings = listOf(
        "Android Wear",
        ANDROID_AUTOMOTIVE_STRING,
    )
)