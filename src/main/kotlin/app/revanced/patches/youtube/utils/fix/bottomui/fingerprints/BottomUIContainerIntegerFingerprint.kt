package app.revanced.patches.youtube.utils.fix.bottomui.fingerprints

import app.revanced.util.fingerprint.LiteralValueFingerprint

internal object BottomUIContainerIntegerFingerprint : LiteralValueFingerprint(
    returnType = "V",
    literalSupplier = { 45637647 }
)