package app.revanced.patches.shared.ads.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

internal object VideoAdsFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf("markFillRequested", "requestEnterSlot")
)