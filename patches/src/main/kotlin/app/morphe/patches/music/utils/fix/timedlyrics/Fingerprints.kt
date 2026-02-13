package app.morphe.patches.music.utils.fix.timedlyrics

import app.morphe.util.fingerprint.legacyFingerprint

internal const val TIMED_LYRICS_PRIMARY_FEATURE_FLAG = 45685201L
internal const val TIMED_LYRICS_SECONDARY_FEATURE_FLAG = 45688384L

internal val timedLyricsPrimaryFingerprint = legacyFingerprint(
    name = "timedLyricsFingerprint",
    returnType = "Z",
    literals = listOf(TIMED_LYRICS_PRIMARY_FEATURE_FLAG),
)

internal val timedLyricsSecondaryFingerprint = legacyFingerprint(
    name = "timedLyricsSecondaryFingerprint",
    returnType = "Z",
    literals = listOf(TIMED_LYRICS_SECONDARY_FEATURE_FLAG),
)
