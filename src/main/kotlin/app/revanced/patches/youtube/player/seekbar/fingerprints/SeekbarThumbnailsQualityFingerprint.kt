package app.revanced.patches.youtube.player.seekbar.fingerprints

import app.revanced.util.fingerprint.LiteralValueFingerprint

internal object SeekbarThumbnailsQualityFingerprint : LiteralValueFingerprint(
    returnType = "Z",
    parameters = emptyList(),
    literalSupplier = { 45399684 },
)