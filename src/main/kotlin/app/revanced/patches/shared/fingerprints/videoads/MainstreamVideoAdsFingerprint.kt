package app.revanced.patches.shared.fingerprints.videoads

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object MainstreamVideoAdsFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf("markFillRequested", "requestEnterSlot")
)