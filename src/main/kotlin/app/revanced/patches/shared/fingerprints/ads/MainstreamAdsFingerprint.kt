package app.revanced.patches.shared.fingerprints.ads

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object MainstreamAdsFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf("markFillRequested", "requestEnterSlot")
)