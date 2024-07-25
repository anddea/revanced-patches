package app.revanced.patches.youtube.utils.fix.bottomui.fingerprints

import app.revanced.util.fingerprint.LiteralValueFingerprint

internal object BottomUIContainerBooleanFingerprint : LiteralValueFingerprint(
    returnType = "Z",
    literalSupplier = { 45637647 }
)