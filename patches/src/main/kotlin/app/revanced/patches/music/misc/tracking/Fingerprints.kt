package app.revanced.patches.music.misc.tracking

import app.revanced.util.fingerprint.legacyFingerprint

/**
 * Sharing panel of Lyrics
 */
internal val imageShareLinkFormatterFingerprint = legacyFingerprint(
    name = "imageShareLinkFormatterFingerprint",
    returnType = "V",
    strings = listOf(
        "image_share",
        "Failed to get URI for lyrics share image.",
        "android.intent.extra.TEXT"
    )
)