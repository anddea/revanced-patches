package app.morphe.patches.youtube.video.playbackstart

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.StringComparisonType
import app.morphe.patcher.methodCall
import app.morphe.patcher.string
import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags

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

internal val shortsPlaybackStartIntentFingerprint = legacyFingerprint(
    name = "shortsPlaybackStartIntentFingerprint",
    accessFlags = AccessFlags.PROTECTED or AccessFlags.FINAL,
    returnType = "V",
    parameters = listOf(
        "Lcom/google/android/libraries/youtube/player/model/PlaybackStartDescriptor;",
        "Ljava/util/Map;",
        "J",
        "Ljava/lang/String;"
    ),
    strings = listOf(
        // None of these strings are unique.
        "com.google.android.apps.youtube.app.endpoint.flags",
        "ReelWatchFragmentArgs",
        "reels_fragment_descriptor"
    )
)

// Pre 19.25
internal val shortsPlaybackStartIntentLegacyFingerprint = legacyFingerprint(
    name = "shortsPlaybackStartIntentLegacyFingerprint",
    returnType = "V",
    parameters = listOf(
        "L",
        "Ljava/util/Map;",
        "J",
        "Ljava/lang/String;",
        "Z",
        "Ljava/util/Map;"
    ),
    strings = listOf(
        // None of these strings are unique.
        "com.google.android.apps.youtube.app.endpoint.flags",
        "ReelWatchFragmentArgs",
        "reels_fragment_descriptor"
    )
)

internal object PlaybackStartDescriptorToStringFingerprint : Fingerprint(
    name = "toString",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/String;",
    filters = listOf(
        methodCall(smali = "Ljava/util/Locale;->getDefault()Ljava/util/Locale;"),
        methodCall(returnType = "Ljava/lang/String;", parameters = listOf()),
        string("PlaybackStartDescriptor:", comparison = StringComparisonType.STARTS_WITH)
    )
)

