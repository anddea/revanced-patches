package app.revanced.patches.shared.spoof.streamingdata

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

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

internal val buildRequestFingerprint = legacyFingerprint(
    name = "buildRequestFingerprint",
    customFingerprint = { method, _ ->
        method.implementation != null &&
                indexOfRequestFinishedListenerInstruction(method) >= 0 &&
                !method.definingClass.startsWith("Lorg/") &&
                indexOfNewUrlRequestBuilderInstruction(method) >= 0 &&
                // Earlier targets
                (indexOfEntrySetInstruction(method) >= 0 ||
                        // Later targets
                        method.parameters[1].type == "Ljava/util/Map;")
    }
)

internal fun indexOfRequestFinishedListenerInstruction(method: Method) =
    method.indexOfFirstInstruction {
        opcode == Opcode.INVOKE_VIRTUAL &&
                getReference<MethodReference>()?.name == "setRequestFinishedListener"
    }

internal fun indexOfNewUrlRequestBuilderInstruction(method: Method) =
    method.indexOfFirstInstruction {
        opcode == Opcode.INVOKE_VIRTUAL &&
                getReference<MethodReference>().toString() == "Lorg/chromium/net/CronetEngine;->newUrlRequestBuilder(Ljava/lang/String;Lorg/chromium/net/UrlRequest${'$'}Callback;Ljava/util/concurrent/Executor;)Lorg/chromium/net/UrlRequest${'$'}Builder;"
    }

internal fun indexOfEntrySetInstruction(method: Method) =
    method.indexOfFirstInstruction {
        opcode == Opcode.INVOKE_INTERFACE &&
                getReference<MethodReference>().toString() == "Ljava/util/Map;->entrySet()Ljava/util/Set;"
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

internal const val HLS_CURRENT_TIME_FEATURE_FLAG = 45355374L

internal val hlsCurrentTimeFingerprint = legacyFingerprint(
    name = "hlsCurrentTimeFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Z", "L"),
    literals = listOf(HLS_CURRENT_TIME_FEATURE_FLAG),
)
