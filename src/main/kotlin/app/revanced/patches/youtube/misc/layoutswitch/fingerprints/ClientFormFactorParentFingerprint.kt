package app.revanced.patches.youtube.misc.layoutswitch.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

object ClientFormFactorParentFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf("ClientFormFactor"),
)