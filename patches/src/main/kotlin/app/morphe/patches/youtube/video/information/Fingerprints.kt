package app.morphe.patches.youtube.video.information

import app.morphe.patches.youtube.utils.PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR
import app.morphe.patches.youtube.utils.YOUTUBE_FORMAT_STREAM_MODEL_CLASS_TYPE
import app.morphe.patches.youtube.utils.YOUTUBE_VIDEO_QUALITY_CLASS_TYPE
import app.morphe.patches.youtube.utils.resourceid.notificationBigPictureIconWidth
import app.morphe.patches.youtube.utils.resourceid.qualityAuto
import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.InstructionLocation.MatchFirst
import app.morphe.patcher.OpcodesFilter
import app.morphe.patcher.StringComparisonType
import app.morphe.patcher.anyInstruction
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string

internal object VideoQualityChangedFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "L",
    parameters = listOf("L"),
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET, type = "I", location = MatchFirst()),
        literal(2, location = MatchAfterImmediately()),
        opcode(Opcode.IF_NE, location = MatchAfterImmediately()),
        opcode(Opcode.NEW_INSTANCE, location = MatchAfterImmediately()), // Obfuscated VideoQuality

        opcode(Opcode.IGET_OBJECT, location = MatchAfterWithin(6)),
        opcode(Opcode.CHECK_CAST),
        fieldAccess(type = "I", opcode = Opcode.IGET, location = MatchAfterImmediately()), // Video resolution (human readable).
    )
)

internal object CreateVideoPlayerSeekbarFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(
        string("timed_markers_width"),
    )
)

internal object OnPlaybackSpeedItemClickParentFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "L",
    parameters = listOf("L", "Ljava/lang/String;"),
    filters = listOf(
        methodCall(name = "getSupportFragmentManager", location = MatchFirst()),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        methodCall(
            returnType = "L",
            parameters = listOf("Ljava/lang/String;"),
            location = MatchAfterImmediately()
        ),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        opcode(Opcode.IF_EQZ, location = MatchAfterImmediately()),
        opcode(Opcode.CHECK_CAST, location = MatchAfterImmediately()),
    ),
    custom = { _, classDef ->
        classDef.methods.count() == 8
    }
)

/**
 * Resolves using the method found in [OnPlaybackSpeedItemClickParentFingerprint].
 */
internal object OnPlaybackSpeedItemClickFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L", "L", "I", "J"),
    custom = { method, _ ->
        method.name == "onItemClick"
    }
)

internal object PlayerControllerSetTimeReferenceFingerprint : Fingerprint(
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.INVOKE_DIRECT_RANGE, Opcode.IGET_OBJECT),
    strings = listOf("Media progress reported outside media playback: ")
)

internal object PlayerInitFingerprint : Fingerprint(
    filters = listOf(
        string("playVideo called on player response with no videoStreamingData."),
    )
)

internal object PlayerStatusEnumFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.STATIC, AccessFlags.CONSTRUCTOR),
    strings = listOf(
        "NEW",
        "PLAYBACK_PENDING",
        "PLAYBACK_LOADED",
        "PLAYBACK_INTERRUPTED",
        "INTERSTITIAL_REQUESTED",
        "INTERSTITIAL_PLAYING",
        "VIDEO_PLAYING",
        "ENDED",
    )
)

/**
 * Matched using class found in [PlayerInitFingerprint].
 */
internal object SeekFingerprint : Fingerprint(
    filters = listOf(
        anyInstruction(
            // 20.xx
            string("Attempting to seek during an ad"),
            // 21.02+
            string("currentPositionMs.")
        )
    )
)

internal object VideoLengthFingerprint : Fingerprint(
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.MOVE_RESULT_WIDE,
        Opcode.CMP_LONG,
        Opcode.IF_LEZ,
        Opcode.IGET_OBJECT,
        Opcode.CHECK_CAST,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_WIDE,
        Opcode.GOTO,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_WIDE,
        Opcode.CONST_4,
        Opcode.INVOKE_VIRTUAL,
    )
)

/**
 * Matches using class found in [MdxPlayerDirectorSetVideoStageFingerprint].
 */
internal object MdxSeekFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf("J", "L"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.RETURN,
    ),
    custom = { methodDef, _ ->
        // The instruction count is necessary here to avoid matching the relative version
        // of the seek method we're after, which has the same function signature as the
        // regular one, is in the same class, and even has the exact same 3 opcodes pattern.
        methodDef.implementation!!.instructions.count() == 3
    }
)

internal object MdxPlayerDirectorSetVideoStageFingerprint : Fingerprint(
    filters = listOf(
        string("MdxDirector setVideoStage ad should be null when videoStage is not an Ad state "),
    )
)

/**
 * Matches using class found in [MdxPlayerDirectorSetVideoStageFingerprint].
 */
internal object MdxSeekRelativeFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    // Return type is boolean up to 19.39, and void with 19.39+.
    parameters = listOf("J", "L"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_INTERFACE,
    )
)

/**
 * Matches using class found in [PlayerInitFingerprint].
 */
internal object SeekRelativeFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    // Return type is boolean up to 19.39, and void with 19.39+.
    parameters = listOf("J", "L"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.ADD_LONG_2ADDR,
        Opcode.INVOKE_VIRTUAL,
    )
)

/**
 * Resolves with the class found in [VideoQualityChangedFingerprint].
 */
internal object PlaybackSpeedMenuSpeedChangedFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "L",
    parameters = listOf("L"),
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET, type = "F")
    )
)

internal object PlaybackSpeedClassFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "L",
    parameters = listOf("L"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.RETURN_OBJECT
    ),
    strings = listOf("PLAYBACK_RATE_MENU_BOTTOM_SHEET_FRAGMENT")
)

/**
 * YouTube 20.19 and lower.
 */
internal object VideoQualityLegacyFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    parameters = listOf(
        "I", // Resolution.
        "Ljava/lang/String;", // Human readable resolution: "480p", "1080p Premium", etc
        "Z",
        "L"
    ),
    custom = { _, classDef ->
        classDef.type == "Lcom/google/android/libraries/youtube/innertube/model/media/VideoQuality;"
    }
)

internal object PlaybackStartDescriptorToStringFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/String;",
    filters = listOf(
        methodCall(smali = "Ljava/util/Locale;->getDefault()Ljava/util/Locale;"),
        // First method call after Locale is the video id.
        methodCall(returnType = "Ljava/lang/String;", parameters = listOf()),
        string("PlaybackStartDescriptor:", comparison = StringComparisonType.STARTS_WITH)
    ),
    custom = { method, _ ->
        method.name == "toString"
    }
)

// Class name is un-obfuscated in targets before 21.01
internal object VideoQualityFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    parameters = listOf(
        "I", // Resolution.
        "L",
        "Ljava/lang/String;", // Human readable resolution: "480p", "1080p Premium", etc
        "Z",
        "L"
    )
)

internal object VideoQualitySetterFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("[L", "I", "Z"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.IF_EQZ,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.IPUT_BOOLEAN,
    ),
    strings = listOf("menu_item_video_quality")
)

/**
 * Matches with the class found in [VideoQualitySetterFingerprint].
 */
internal object SetVideoQualityFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("L"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.IGET_OBJECT,
        Opcode.IPUT_OBJECT,
        Opcode.IGET_OBJECT,
    )
)

internal val channelIdFingerprint = legacyFingerprint(
    name = "channelIdFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Ljava/lang/Object;"),
    strings = listOf("com.google.android.apps.youtube.mdx.watch.LAST_MEALBAR_PROMOTED_LIVE_FEED_CHANNELS")
)

internal val channelNameFingerprint = legacyFingerprint(
    name = "channelNameFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    strings = listOf(
        "setMetadata may only be called once",
        "Person",
    )
)

internal val onPlaybackSpeedItemClickFingerprint = legacyFingerprint(
    name = "onPlaybackSpeedItemClickFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "V",
    parameters = listOf("Landroid/widget/AdapterView;", "Landroid/view/View;", "I", "J"),
    customFingerprint = { method, _ ->
        method.name == "onItemClick" &&
                method.indexOfFirstInstruction {
                    opcode == Opcode.IGET_OBJECT &&
                            getReference<FieldReference>()?.type == PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR
                } >= 0
    }
)

internal val playbackInitializationFingerprint = legacyFingerprint(
    name = "playbackInitializationFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    strings = listOf("play() called when the player wasn\'t loaded."),
    customFingerprint = { method, _ ->
        indexOfPlayerResponseModelDirectInstruction(method) >= 0
    }
)

internal fun indexOfPlayerResponseModelDirectInstruction(method: Method) =
    method.indexOfFirstInstruction {
        opcode == Opcode.INVOKE_DIRECT &&
                getReference<MethodReference>()?.returnType == PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR
    }

internal val playbackSpeedClassFingerprint = legacyFingerprint(
    name = "playbackSpeedClassFingerprint",
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    parameters = listOf("L"),
    opcodes = listOf(Opcode.RETURN_OBJECT),
    strings = listOf("PLAYBACK_RATE_MENU_BOTTOM_SHEET_FRAGMENT")
)

internal val playerControllerSetTimeReferenceFingerprint = legacyFingerprint(
    name = "playerControllerSetTimeReferenceFingerprint",
    opcodes = listOf(
        Opcode.INVOKE_DIRECT_RANGE,
        Opcode.IGET_OBJECT
    ),
    strings = listOf("Media progress reported outside media playback: ")
)

internal val seekRelativeFingerprint = legacyFingerprint(
    name = "seekRelativeFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    // returnType = "Z", ~ YouTube 19.39.39
    // returnType = "V", YouTube 19.40.xx ~
    parameters = listOf("J", "L"),
    opcodes = listOf(
        Opcode.ADD_LONG_2ADDR,
        Opcode.INVOKE_VIRTUAL,
    )
)

internal val videoIdFingerprint = legacyFingerprint(
    name = "videoIdFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    strings = listOf("Failed to download video (IllegalStateException): %s")
)

/**
 * Renamed from VideoIdWithoutShortsFingerprint
 */
internal val videoIdFingerprintBackgroundPlay = legacyFingerprint(
    name = "videoIdFingerprintBackgroundPlay",
    returnType = "V",
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.IF_EQZ,
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IPUT_OBJECT,
        Opcode.MONITOR_EXIT,
        Opcode.RETURN_VOID,
        Opcode.MONITOR_EXIT,
        Opcode.RETURN_VOID
    ),
    customFingerprint = { method, classDef ->
        method.name == "l" &&
                classDef.methods.count() == 17 &&
                method.implementation != null &&
                indexOfPlayerResponseModelInterfaceInstruction(method) >= 0
    }
)

fun indexOfPlayerResponseModelInterfaceInstruction(method: Method) =
    method.indexOfFirstInstruction {
        opcode == Opcode.INVOKE_INTERFACE &&
                getReference<MethodReference>()?.definingClass == PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR
    }

internal val videoTitleFingerprint = legacyFingerprint(
    name = "videoTitleFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    literals = listOf(notificationBigPictureIconWidth),
)

internal val videoQualityFingerprint = legacyFingerprint(
    name = "videoQualityFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    opcodes = listOf(
        Opcode.IPUT_BOOLEAN,
        Opcode.IPUT_OBJECT,
        Opcode.RETURN_VOID,
    ),
    customFingerprint = { method, classDef ->
        classDef.type == YOUTUBE_VIDEO_QUALITY_CLASS_TYPE &&
                method.parameterTypes.size > 3 &&
                indexOfVideoQualityNameFieldInstruction(method) >= 0 &&
                indexOfVideoQualityResolutionFieldInstruction(method) >= 0
    }
)

fun indexOfVideoQualityNameFieldInstruction(method: Method) =
    method.indexOfFirstInstruction {
        val reference = getReference<FieldReference>()
        opcode == Opcode.IPUT_OBJECT &&
                reference?.type == "Ljava/lang/String;" &&
                reference.definingClass == YOUTUBE_VIDEO_QUALITY_CLASS_TYPE
    }

fun indexOfVideoQualityResolutionFieldInstruction(method: Method) =
    method.indexOfFirstInstruction {
        val reference = getReference<FieldReference>()
        opcode == Opcode.IPUT &&
                reference?.type == "I" &&
                reference.definingClass == YOUTUBE_VIDEO_QUALITY_CLASS_TYPE
    }

internal val videoQualitySetterFingerprint = legacyFingerprint(
    name = "videoQualitySetterFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("[L", "I", "Z"),
    opcodes = listOf(
        Opcode.IF_GE,
        Opcode.AGET_OBJECT,
        Opcode.IGET_OBJECT,
    ),
    strings = listOf("menu_item_video_quality")
)

internal val videoQualityListFingerprint = legacyFingerprint(
    name = "videoQualityListFingerprint",
    returnType = "V",
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.INVOKE_INTERFACE,
        Opcode.RETURN_VOID
    ),
    literals = listOf(qualityAuto),
)

internal val availableVideoFormatsFingerprint = legacyFingerprint(
    name = "availableVideoFormatsFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.STATIC,
    parameters = listOf("Ljava/util/List;", "I"),
    opcodes = listOf(
        Opcode.CHECK_CAST,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT
    ),
)

internal val formatStreamModelBuilderFingerprint = legacyFingerprint(
    name = "formatStreamModelBuilderFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "L",
    strings = listOf("vprng")
)

internal val formatStreamingModelQualityLabelBuilderFingerprint = legacyFingerprint(
    name = "formatStreamingModelQualityLabelBuilderFingerprint",
    returnType = "Ljava/lang/String;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    strings = listOf("60")
)

internal val initFormatStreamParentFingerprint = legacyFingerprint(
    name = "initFormatStreamParentFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    returnType = "V",
    strings = listOf("noopytm")
)

internal val initFormatStreamFingerprint = legacyFingerprint(
    name = "initFormatStreamFingerprint",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.FINAL,
    returnType = "V",
    customFingerprint = { method, _ ->
        indexOfPreferredFormatStreamInstruction(method) >= 0
    }
)

internal fun indexOfPreferredFormatStreamInstruction(method: Method) =
    method.indexOfFirstInstruction {
        opcode == Opcode.IGET_OBJECT &&
                getReference<FieldReference>()?.type == YOUTUBE_FORMAT_STREAM_MODEL_CLASS_TYPE
    }

internal val videoQualityArrayFingerprint = legacyFingerprint(
    name = "videoQualityArrayFingerprint",
    returnType = "Ljava/util/List;",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.STATIC or AccessFlags.FINAL,
    parameters = listOf("Ljava/util/List;", "L"),
    opcodes = listOf(Opcode.RETURN_OBJECT)
)
