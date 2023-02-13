package app.revanced.patches.youtube.misc.microg.bytecode.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object ServiceCheckFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf("Google Play Services not available", "GooglePlayServices not available due to error ")
)