package app.morphe.patches.shared.spoof.streamingdata

import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.indexOfFirstInstructionReversed
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

// In YouTube 17.34.36, this class is obfuscated.
const val STREAMING_DATA_OUTER_CLASS =
    "Lcom/google/protos/youtube/api/innertube/StreamingDataOuterClass${'$'}StreamingData;"

internal val buildInitPlaybackRequestFingerprint = legacyFingerprint(
    name = "buildInitPlaybackRequestFingerprint",
    returnType = "Lorg/chromium/net/UrlRequest\$Builder;",
    opcodes = listOf(
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IGET_OBJECT, // Moves the request URI string to a register to build the request with.
    ),
    strings = listOf(
        "Content-Type",
        "Range",
    ),
    customFingerprint = { method, _ ->
        indexOfUriToStringInstruction(method) >= 0
    },
)

internal val buildMediaDataSourceFingerprint = legacyFingerprint(
    name = "buildMediaDataSourceFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    returnType = "V",
    parameters = listOf(
        "Landroid/net/Uri;",
        "J",
        "I",
        "[B",
        "Ljava/util/Map;",
        "J",
        "J",
        "Ljava/lang/String;",
        "I",
        "Ljava/lang/Object;"
    )
)

internal val buildPlayerRequestURIFingerprint = legacyFingerprint(
    name = "buildPlayerRequestURIFingerprint",
    returnType = "Ljava/lang/String;",
    strings = listOf(
        "key",
        "asig",
    ),
    customFingerprint = { method, _ ->
        indexOfUriToStringInstruction(method) >= 0
    },
)

internal fun indexOfUriToStringInstruction(method: Method) =
    method.indexOfFirstInstruction {
        opcode == Opcode.INVOKE_VIRTUAL &&
                getReference<MethodReference>().toString() == "Landroid/net/Uri;->toString()Ljava/lang/String;"
    }

internal val createStreamingDataFingerprint = legacyFingerprint(
    name = "createStreamingDataFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    returnType = "V",
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.IPUT_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.IF_NEZ,
        Opcode.SGET_OBJECT,
        Opcode.IPUT_OBJECT
    ),
)

internal val createStreamingDataParentFingerprint = legacyFingerprint(
    name = "createStreamingDataParentFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "L",
    parameters = emptyList(),
    strings = listOf("Invalid playback type; streaming data is not playable"),
)

internal val getEmptyRegistryFingerprint = legacyFingerprint(
    name = "getEmptyRegistryFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    parameters = emptyList(),
    returnType = "Lcom/google/protobuf/ExtensionRegistryLite;",
    customFingerprint = { method, classDef ->
        classDef.type == "Lcom/google/protobuf/ExtensionRegistryLite;"
                && method.name != "getGeneratedRegistry"
    },
)

internal val protobufClassParseByteBufferFingerprint = legacyFingerprint(
    name = "protobufClassParseByteBufferFingerprint",
    accessFlags = AccessFlags.PROTECTED or AccessFlags.STATIC,
    parameters = listOf("L", "Ljava/nio/ByteBuffer;"),
    returnType = "L",
    opcodes = listOf(
        Opcode.SGET_OBJECT,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.RETURN_OBJECT,
    ),
    customFingerprint = { method, _ -> method.name == "parseFrom" },
)

internal val nerdsStatsFormatBuilderFingerprint = legacyFingerprint(
    name = "nerdsStatsFormatBuilderFingerprint",
    returnType = "Ljava/lang/String;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    parameters = listOf("L"),
    strings = listOf("codecs=\""),
)

internal val videoStreamingDataConstructorFingerprint = legacyFingerprint(
    name = "videoStreamingDataConstructorFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    returnType = "V",
    customFingerprint = { method, _ ->
        indexOfGetAdaptiveFormatsFieldInstruction(method) >= 0
    },
)

internal fun indexOfGetAdaptiveFormatsFieldInstruction(method: Method) =
    method.indexOfFirstInstruction {
        val reference = getReference<FieldReference>()
        opcode == Opcode.IGET_OBJECT &&
                reference?.definingClass == STREAMING_DATA_OUTER_CLASS &&
                // Field f: 'adaptiveFormats'.
                // Field name is always 'f', regardless of the client version.
                reference.name == "f" &&
                reference.type.startsWith("L")
    }

/**
 * On YouTube, this class is 'Lcom/google/android/libraries/youtube/innertube/model/media/VideoStreamingData;'
 * On YouTube Music, class names are obfuscated.
 */
internal val videoStreamingDataToStringFingerprint = legacyFingerprint(
    name = "videoStreamingDataToStringFingerprint",
    returnType = "Ljava/lang/String;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    strings = listOf("VideoStreamingData(itags="),
    customFingerprint = { method, _ ->
        method.name == "toString"
    },
)

internal const val DISABLED_BY_SABR_STREAMING_URI_STRING = "DISABLED_BY_SABR_STREAMING_URI"

internal val mediaFetchEnumConstructorFingerprint = legacyFingerprint(
    name = "mediaFetchEnumConstructorFingerprint",
    returnType = "V",
    strings = listOf(
        "ENABLED",
        "DISABLED_FOR_PLAYBACK",
        DISABLED_BY_SABR_STREAMING_URI_STRING
    )
)

internal const val HLS_CURRENT_TIME_FEATURE_FLAG = 45355374L

internal val hlsCurrentTimeFingerprint = legacyFingerprint(
    name = "hlsCurrentTimeFingerprint",
    parameters = listOf("Z", "L"),
    literals = listOf(HLS_CURRENT_TIME_FEATURE_FLAG),
)

// Feature flag that turns on Platypus programming language code compiled to native C++.
// This code appears to replace the player config after the streams are loaded.
// Flag is present in YouTube 19.34, but is missing Platypus stream replacement code until 19.43.
// Flag and Platypus code is also present in newer versions of YouTube Music.
internal const val MEDIA_FETCH_HOT_CONFIG_FEATURE_FLAG = 45645570L

internal val mediaFetchHotConfigFingerprint = legacyFingerprint(
    name = "mediaFetchHotConfigFingerprint",
    literals = listOf(MEDIA_FETCH_HOT_CONFIG_FEATURE_FLAG),
)

// YouTube 20.10 ~ / YouTube Music 8.12 ~
internal const val MEDIA_FETCH_HOT_CONFIG_ALTERNATIVE_FEATURE_FLAG = 45683169L

internal val mediaFetchHotConfigAlternativeFingerprint = legacyFingerprint(
    name = "mediaFetchHotConfigAlternativeFingerprint",
    literals = listOf(MEDIA_FETCH_HOT_CONFIG_ALTERNATIVE_FEATURE_FLAG),
)

// Feature flag that enables different code for parsing and starting video playback,
// but it's exact purpose is not known. If this flag is enabled while stream spoofing
// then videos will never start playback and load forever.
// Flag does not seem to affect playback if spoofing is off.
// YouTube 19.50 ~ / YouTube Music 7.33 ~
internal const val PLAYBACK_START_CHECK_ENDPOINT_USED_FEATURE_FLAG = 45665455L

internal val playbackStartDescriptorFeatureFlagFingerprint = legacyFingerprint(
    name = "playbackStartDescriptorFeatureFlagFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    returnType = ("Z"),
    literals = listOf(PLAYBACK_START_CHECK_ENDPOINT_USED_FEATURE_FLAG)
)

internal val progressBarVisibilityFingerprint = legacyFingerprint(
    name = "progressBarVisibilityFingerprint",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.FINAL,
    parameters = emptyList(),
    returnType = ("V"),
    customFingerprint = { method, _ ->
        indexOfProgressBarVisibilityInstruction(method) >= 0
    }
)

internal fun indexOfProgressBarVisibilityInstruction(method: Method) =
    method.indexOfFirstInstructionReversed {
        opcode == Opcode.INVOKE_VIRTUAL &&
                getReference<MethodReference>()?.toString() == "Landroid/widget/ProgressBar;->setVisibility(I)V"
    }

internal val progressBarVisibilityParentFingerprint = legacyFingerprint(
    name = "progressBarVisibilityParentFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    returnType = ("V"),
    literals = listOf(playerLoadingViewThin)
)
