package app.revanced.patches.music.misc.tracking

import app.revanced.util.fingerprint.legacyFingerprint

/**
 * Sharing panel of Lyrics
 */
internal val imageShareLinkFormatterFingerprint = legacyFingerprint(
    name = "imageShareLinkFormatterFingerprint",
    returnType = "V",
    strings = listOf(
        "android.intent.extra.TEXT",
        "Image Uri is null.",
    )
)