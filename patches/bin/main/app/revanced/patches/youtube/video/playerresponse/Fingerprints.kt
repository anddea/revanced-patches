package app.revanced.patches.youtube.video.playerresponse

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import app.revanced.util.parametersEqual
import com.android.tools.smali.dexlib2.AccessFlags

private val PLAYER_PARAMETER_STARTS_WITH_PARAMETER_LIST = listOf(
    "Ljava/lang/String;", // VideoId.
    "[B",
    "Ljava/lang/String;", // Player parameters proto buffer.
    "Ljava/lang/String;", // PlaylistId.
    "I",
    "I"
)

internal val playerParameterBuilderFingerprint = legacyFingerprint(
    name = "playerParameterBuilderFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "L",
    strings = listOf("psps"),
    // 19.22 and earlier parameters are:
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

    // 19.23+ parameters are:
    // "Ljava/lang/String;", // VideoId.
    // "[B",
    // "Ljava/lang/String;", // Player parameters proto buffer.
    // "Ljava/lang/String;", // PlaylistId.
    // "I",
    // "I",
    // "L",
    // "Ljava/util/Set;",
    // "Ljava/lang/String;",
    // "Ljava/lang/String;",
    // "L",
    // "Z", // Appears to indicate if the video id is being opened or is currently playing.
    // "Z",
    // "Z"
    customFingerprint = custom@{ method, _ ->
        val parameterTypes = method.parameterTypes
        val parameterSize = parameterTypes.size
        if (parameterSize < 13) {
            return@custom false
        }

        val startsWithMethodParameterList = parameterTypes.slice(0..5)

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