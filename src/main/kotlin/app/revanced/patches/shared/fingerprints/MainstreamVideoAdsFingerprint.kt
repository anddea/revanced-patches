package app.revanced.patches.shared.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object MainstreamVideoAdsFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf("markFillRequested", "requestEnterSlot")
)