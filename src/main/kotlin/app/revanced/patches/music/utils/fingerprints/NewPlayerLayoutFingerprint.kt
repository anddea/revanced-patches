package app.revanced.patches.music.utils.fingerprints

import app.revanced.util.fingerprint.LiteralValueFingerprint

object NewPlayerLayoutFingerprint : LiteralValueFingerprint(
    returnType = "Z",
    parameters = emptyList(),
    literalSupplier = { 45399578 }
)