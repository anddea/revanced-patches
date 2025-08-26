package app.revanced.patches.music.utils.fix.timedlyrics

import app.revanced.util.fingerprint.legacyFingerprint

internal const val TIMED_LYRICS_FEATURE_FLAG = 45685201L

internal val timedLyricsFingerprint = legacyFingerprint(
    name = "timedLyricsFingerprint",
    returnType = "Z",
    literals = listOf(TIMED_LYRICS_FEATURE_FLAG),
)
