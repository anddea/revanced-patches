package app.morphe.patches.music.video.playerresponse

import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.or
import app.morphe.util.parametersEqual
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

private val PLAYER_PARAMETER_STARTS_WITH_PARAMETER_LIST = listOf(
    "Ljava/lang/String;", // VideoId.
    "[B",
    "Ljava/lang/String;", // Player parameters proto buffer.
    "Ljava/lang/String;", // PlaylistId.
    "I"                  // PlaylistIndex.
)

/**
 * For targets 7.03 and later.
 */
internal val playerParameterBuilderFingerprint = legacyFingerprint(
    name = "playerParameterBuilderFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "L",
    strings = listOf("psps"),
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
 * For targets 7.02 and earlier.
 */
internal val playerParameterBuilderLegacyFingerprint = legacyFingerprint(
    name = "playerParameterBuilderLegacyFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "L",
    parameters = listOf(
        "Ljava/lang/String;", // VideoId.
        "[B",
        "Ljava/lang/String;", // Player parameters proto buffer.
        "Ljava/lang/String;", // PlaylistId.
        "I",                  // PlaylistIndex.
        "I",
        "Ljava/util/Set;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "L",
        "Z",
        "Z", // Appears to indicate if the video id is being opened or is currently playing.
    ),
    opcodes = listOf(
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CHECK_CAST,
        Opcode.INVOKE_INTERFACE
    )
)