package app.revanced.patches.music.general.components.fingerprints

import app.revanced.util.fingerprint.MethodReferenceNameFingerprint

object SearchBarFingerprint : MethodReferenceNameFingerprint(
    returnType = "V",
    reference = { "setVisibility" }
)