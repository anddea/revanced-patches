package app.revanced.patches.music.video.playerresponse

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

/**
 * For targets 7.03 and later.
 */
internal val playerParameterBuilderFingerprint = legacyFingerprint(
    name = "playerParameterBuilderFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "L",
    parameters = listOf(
        "Ljava/lang/String;", // VideoId.
        "[B",
        "Ljava/lang/String;", // Player parameters proto buffer.
        "Ljava/lang/String;", // PlaylistId.
        "I",                  // PlaylistIndex.
        "I",
        "L",
        "Ljava/util/Set;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "L",
        "Z",
        "Z",
        "Z", // Appears to indicate if the video id is being opened or is currently playing.
    ),
    strings = listOf("psps")
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