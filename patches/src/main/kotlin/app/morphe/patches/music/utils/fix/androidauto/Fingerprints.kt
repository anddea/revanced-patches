package app.morphe.patches.music.utils.fix.androidauto

import app.morphe.util.fingerprint.legacyFingerprint

internal val certificateCheckFingerprint = legacyFingerprint(
    name = "certificateCheckFingerprint",
    returnType = "Z",
    parameters = listOf("L"),
    strings = listOf("X509")
)