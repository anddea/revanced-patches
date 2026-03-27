package app.morphe.patches.youtube.video.playerresponse

import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.or
import app.morphe.util.parametersEqual
import com.android.tools.smali.dexlib2.AccessFlags

private val PLAYER_PARAMETER_STARTS_WITH_PARAMETER_LIST = listOf(
    "Ljava/lang/String;", // VideoId.
    "[B",
    "Ljava/lang/String;", // Player parameters proto buffer.
    "Ljava/lang/String;", // PlaylistId.
    "I"
)

internal val playerParameterBuilderFingerprint = legacyFingerprint(
    name = "playerParameterBuilderFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "L",
    strings = listOf("psps"),
    // parameters in 18.29 ~ 19.22 :
    // "Ljava/lang/String;", // VideoId.
    // "[B",
    // "Ljava/lang/String;", // Player parameters proto buffer.
    // "Ljava/lang/String;", // PlaylistId.
    // "I",
    // "I",
    // "Ljava/util/Set;",
    // "Ljava/lang/String;",
    // "Ljava/lang/String;",
    // "L",
    // "Z", // Appears to indicate if the video id is being opened or is currently playing.
    // "Z",
    // "Z"

    // parameters in 19.23 ~ 20.09 :
    // "Ljava/lang/String;", // VideoId.
    // "[B",
    // "Ljava/lang/String;", // Player parameters proto buffer.
    // "Ljava/lang/String;", // PlaylistId.
    // "I",
    // "I",
    // "L", // New parameters added in 19.25.
    // "Ljava/util/Set;",
    // "Ljava/lang/String;",
    // "Ljava/lang/String;",
    // "L",
    // "Z", // Appears to indicate if the video id is being opened or is currently playing.
    // "Z",
    // "Z",
    // "Z"

    // parameters in 20.10 ~ :
    // "Ljava/lang/String;", // VideoId.
    // "[B",
    // "Ljava/lang/String;", // Player parameters proto buffer.
    // "Ljava/lang/String;", // PlaylistId.
    // "I",
    // "Z", // New parameters added in 20.10.
    // "I",
    // "L", // New parameters added in 19.25.
    // "Ljava/util/Set;",
    // "Ljava/lang/String;",
    // "Ljava/lang/String;",
    // "L",
    // "Z", // Appears to indicate if the video id is being opened or is currently playing.
    // "Z",
    // "Z",
    // "Z"
    customFingerprint = custom@{ method, _ ->
        val parameterTypes = method.parameterTypes
        val parameterSize = parameterTypes.size
        if (parameterSize < 13) {
            return@custom false
        }

        val startsWithMethodParameterList = parameterTypes.slice(0..4)

        parametersEqual(
            PLAYER_PARAMETER_STARTS_WITH_PARAMETER_LIST,
            startsWithMethodParameterList
        )
    }
)

/**
 * For targets 19.22 and earlier.
 */
private val PLAYER_PARAMETER_LEGACY_LIST = listOf(
    "Ljava/lang/String;", // VideoId.
    "[B",
    "Ljava/lang/String;", // Player parameters proto buffer.
    "Ljava/lang/String;",
    "I",
    "I",
    "Ljava/util/Set;",
    "Ljava/lang/String;",
    "Ljava/lang/String;",
    "L",
    "Z", // Appears to indicate if the video id is being opened or is currently playing.
    "Z",
    "Z",
)

internal val playerParameterBuilderLegacyFingerprint = legacyFingerprint(
    name = "playerParameterBuilderLegacyFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "L",
    parameters = PLAYER_PARAMETER_LEGACY_LIST,
)