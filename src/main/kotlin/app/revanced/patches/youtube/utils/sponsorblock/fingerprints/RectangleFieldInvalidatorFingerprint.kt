package app.revanced.patches.youtube.utils.sponsorblock.fingerprints

import app.revanced.util.fingerprint.MethodReferenceNameFingerprint

internal object RectangleFieldInvalidatorFingerprint : MethodReferenceNameFingerprint(
    returnType = "V",
    parameters = emptyList(),
    reference = { "invalidate" }
)
