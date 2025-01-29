package app.revanced.patches.youtube.video.playbackstart

import app.revanced.util.fingerprint.legacyFingerprint

const val PLAYBACK_START_DESCRIPTOR_CLASS_DESCRIPTOR =
    "Lcom/google/android/libraries/youtube/player/model/PlaybackStartDescriptor;"

/**
 * Purpose of this method is not clear, and it's only used to identify
 * the obfuscated name of the videoId() method in PlaybackStartDescriptor.
 */
internal val playbackStartFeatureFlagFingerprint = legacyFingerprint(
    name = "playbackStartFeatureFlagFingerprint",
    returnType = "Z",
    parameters = listOf(PLAYBACK_START_DESCRIPTOR_CLASS_DESCRIPTOR),
    literals = listOf(45380134L)
)


