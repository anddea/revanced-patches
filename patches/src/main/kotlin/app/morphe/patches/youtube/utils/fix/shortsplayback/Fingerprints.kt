package app.morphe.patches.youtube.utils.fix.shortsplayback

import app.morphe.util.fingerprint.legacyFingerprint

internal const val SHORTS_PLAYBACK_PRIMARY_FEATURE_FLAG = 45387052L

internal val shortsPlaybackPrimaryFingerprint = legacyFingerprint(
    name = "shortsPlaybackPrimaryFingerprint",
    returnType = "Z",
    literals = listOf(SHORTS_PLAYBACK_PRIMARY_FEATURE_FLAG),
)

internal const val SHORTS_PLAYBACK_SECONDARY_FEATURE_FLAG = 45378771L

internal val shortsPlaybackSecondaryFingerprint = legacyFingerprint(
    name = "shortsPlaybackSecondaryFingerprint",
    returnType = "L",
    literals = listOf(SHORTS_PLAYBACK_SECONDARY_FEATURE_FLAG),
)