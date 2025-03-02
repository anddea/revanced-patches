package app.revanced.patches.youtube.utils.fix.streamingdata

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

// In YouTube 17.34.36, this class is obfuscated.
const val STREAMING_DATA_INTERFACE =
    "Lcom/google/protos/youtube/api/innertube/StreamingDataOuterClass${'$'}StreamingData;"

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

internal val nerdsStatsVideoFormatBuilderFingerprint = legacyFingerprint(
    name = "nerdsStatsVideoFormatBuilderFingerprint",
    returnType = "Ljava/lang/String;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    parameters = listOf("L"),
    strings = listOf("codecs=\""),
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

internal val videoStreamingDataConstructorFingerprint = legacyFingerprint(
    name = "videoStreamingDataConstructorFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    returnType = "V",
    customFingerprint = { method, _ ->
        indexOfGetFormatsFieldInstruction(method) >= 0 &&
                indexOfLongMaxValueInstruction(method) >= 0 &&
                indexOfFormatStreamModelInitInstruction(method) >= 0
    },
)

internal fun indexOfGetFormatsFieldInstruction(method: Method) =
    method.indexOfFirstInstruction {
        val reference = getReference<FieldReference>()
        opcode == Opcode.IGET_OBJECT &&
                reference?.definingClass == STREAMING_DATA_INTERFACE &&
                // Field e: 'formats'.
                // Field name is always 'e', regardless of the client version.
                reference.name == "e" &&
                reference.type.startsWith("L")
    }

internal fun indexOfLongMaxValueInstruction(method: Method, index: Int = 0) =
    method.indexOfFirstInstruction(index) {
        (this as? WideLiteralInstruction)?.wideLiteral == Long.MAX_VALUE
    }

internal fun indexOfFormatStreamModelInitInstruction(method: Method) =
    method.indexOfFirstInstruction {
        val reference = getReference<MethodReference>()
        opcode == Opcode.INVOKE_DIRECT &&
                reference?.name == "<init>" &&
                reference.parameterTypes.size > 1
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

internal const val HLS_CURRENT_TIME_FEATURE_FLAG = 45355374L

internal val hlsCurrentTimeFingerprint = legacyFingerprint(
    name = "hlsCurrentTimeFingerprint",
    parameters = listOf("Z", "L"),
    literals = listOf(HLS_CURRENT_TIME_FEATURE_FLAG),
)


