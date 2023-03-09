package app.revanced.patches.youtube.layout.general.pivotbar.shortspivotbar.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object ReelWatchEndpointFingerprint : MethodFingerprint(
    returnType = "V",
    customFingerprint = { it.name == "<init>" }
)