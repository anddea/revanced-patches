package app.revanced.patches.music.misc.androidauto.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object CertificateCheckFingerprint : MethodFingerprint(
    returnType = "Z",
    parameters = listOf("L"),
    strings = listOf("X509")
)