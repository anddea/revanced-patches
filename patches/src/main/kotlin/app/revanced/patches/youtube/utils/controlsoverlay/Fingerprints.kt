package app.revanced.patches.youtube.utils.controlsoverlay

import app.revanced.util.fingerprint.legacyFingerprint

/**
 * Added in YouTube v18.39.41
 *
 * When this value is TRUE, new control overlay is used.
 * In this case, the associated patches no longer work, so set this value to FALSE.
 */
internal val controlsOverlayConfigFingerprint = legacyFingerprint(
    name = "controlsOverlayConfigFingerprint",
    returnType = "Z",
    literals = listOf(45427491L),
)
