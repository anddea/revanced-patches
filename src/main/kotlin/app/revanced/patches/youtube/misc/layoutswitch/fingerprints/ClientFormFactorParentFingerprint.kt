package app.revanced.patches.youtube.misc.layoutswitch.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object ClientFormFactorParentFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf("ClientFormFactor"),
)