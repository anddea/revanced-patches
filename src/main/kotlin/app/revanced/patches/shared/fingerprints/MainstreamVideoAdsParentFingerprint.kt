package app.revanced.patches.shared.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object MainstreamVideoAdsParentFingerprint : MethodFingerprint(
    strings = listOf("exitSlot")
)
