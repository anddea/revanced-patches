/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.video.playback

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.checkCast
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

/**
 * 'Video quality preferences' setting of YouTube settings
 * (Auto, Higher picture quality, Data saver or Advanced).
 */
internal object VideoQualitySettingEnumFingerprint : Fingerprint(
    name = "<clinit>",
    filters = listOf(
        string("VIDEO_QUALITY_SETTING_ADVANCED_MENU"),
        fieldAccess( // VIDEO_QUALITY_SETTING_ADVANCED_MENU enum value.
            opcode = Opcode.SPUT_OBJECT,
            definingClass = "this",
            location = MatchAfterWithin(5)
        )
    )
)

/**
 * Video quality setting used for a video ID, sent with the SABR requests.
 */
internal fun getVideoQualitySettingFingerprint(
    videoQualitySettingType: String
) = object : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = videoQualitySettingType,
    parameters = listOf("Ljava/lang/String;"),
    filters = listOf(
        methodCall(
            definingClass = "Ljava/util/Map;",
            name = "get"
        ),
        checkCast(videoQualitySettingType)
    )
) {}

internal const val CLIENT_ABR_STATE_CLASS =
    "Lcom/google/android/apps/youtube/proto/ClientAbrStateOuterClass\$ClientAbrState;"

/**
 * Client ABR state of the SABR requests sent while the video plays.
 */
internal object ClientAbrStateFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = CLIENT_ABR_STATE_CLASS,
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            definingClass = CLIENT_ABR_STATE_CLASS,
            type = "Lcom/google/android/apps/youtube/proto/streaming/MediaCapabilitiesOuterClass\$MediaCapabilities;"
        )
    )
)

/**
 * Quality last selected in the quality menu, kept by YouTube for the next videos.
 * The player uses it as a fixed quality, and it is sent with the SABR requests.
 * The direction is how the selected quality compares to the quality playing when it was selected.
 */
internal object LastSelectedVideoQualityConstructorFingerprint : Fingerprint(
    name = "<init>",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    parameters = listOf("L", "I", "L"),
    filters = listOf(
        fieldAccess( // Time the quality was selected.
            opcode = Opcode.IPUT_WIDE,
            definingClass = "this",
            type = "J"
        ),
        fieldAccess( // Video ID the quality was selected for.
            opcode = Opcode.IPUT_OBJECT,
            definingClass = "this",
            type = "Ljava/lang/String;"
        ),
        string("last_playback_start_timestamp"),
        fieldAccess( // Time the last playback started.
            opcode = Opcode.IPUT_WIDE,
            definingClass = "this",
            type = "J"
        ),
        fieldAccess( // Resolution of the quality.
            opcode = Opcode.IPUT,
            definingClass = "this",
            type = "I"
        ),
        fieldAccess( // Direction of the quality change.
            opcode = Opcode.IPUT,
            definingClass = "this",
            type = "I"
        )
    )
)

/**
 * Chooses the qualities allowed by the player, when the video starts and again while it plays:
 * the quality last selected in the quality menu, or an automatic quality.
 */
internal fun getVideoQualityConstraintFingerprint(
    lastSelectedVideoQualityType: String,
    videoQualitySettingMethod: String
) = object : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Z", "L", "Ljava/lang/String;", "L"),
    filters = listOf(
        methodCall(returnType = lastSelectedVideoQualityType),
        methodCall(smali = videoQualitySettingMethod)
    )
) {}

/**
 * If this flag is off, the qualities of the quality menu have no formats, and the player ignores
 * the formats of the quality last selected and finds the quality only by its resolution.
 */
internal object QualityFormatsFeatureFlagFingerprint : Fingerprint(
    filters = listOf(
        literal(45400072)
    )
)

/**
 * The method of the player that sets the video formats to play, and clears the buffered video
 * if the parameter is true.
 */
internal object PlaybackControllerSetVideoFormatsFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/libraries/youtube/media/player/exo2/platypus/PlaybackControllerWrapper;",
    accessFlags = listOf(AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf("Z"),
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            type = "Lcom/google/android/libraries/youtube/media/interfaces/PlaybackController;"
        ),
        opcode(Opcode.IF_NEZ, location = MatchAfterImmediately()),
        // The parameter, if the buffered video is cleared.
        opcode(Opcode.IF_EQZ, location = MatchAfterWithin(3)),
        opcode(Opcode.SGET_OBJECT, location = MatchAfterImmediately()) // Video track type.
    )
)

/**
 * The method of the async media fetch controller that sets the video formats to play.
 * If the flag is on, the method sets the formats itself instead of calling
 * [PlaybackControllerSetVideoFormatsFingerprint], and clears the buffered video
 * if the boolean parameter is true.
 */
internal object AsyncMediaFetchControllerSetVideoFormatsFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/libraries/youtube/media/player/exo2/platypus/AsyncMediaFetchControllerWrapper;",
    returnType = "V",
    parameters = listOf("L", "Z"),
    filters = listOf(
        literal(45779716)
    )
)
