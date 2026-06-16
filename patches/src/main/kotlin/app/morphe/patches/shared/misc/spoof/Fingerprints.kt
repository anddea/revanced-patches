package app.morphe.patches.shared.misc.spoof

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object BuildInitPlaybackRequestFingerprint : Fingerprint(
    returnType = $$"Lorg/chromium/net/UrlRequest$Builder;",
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IGET_OBJECT,
    ),
    strings = listOf(
        "Content-Type",
        "Range",
    )
)

internal object BuildPlayerRequestURIBuilderFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.FINAL),
    returnType = $$"Landroid/net/Uri$Builder;",
    parameters = listOf(),
    filters = listOf(
        string("key"),
        string("asig"),
        methodCall($$"Landroid/net/Uri$Builder;->appendQueryParameter(Ljava/lang/String;Ljava/lang/String;)Landroid/net/Uri$Builder;"),
        opcode(Opcode.RETURN_OBJECT)
    )
)

internal object BuildPlayerRequestURIFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;",
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IPUT_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.MONITOR_EXIT,
        Opcode.RETURN_OBJECT,
    ),
    strings = listOf(
        "key",
        "asig",
    )
)

internal object BuildRequestFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Lorg/chromium/net/UrlRequest",
    filters = listOf(
        methodCall(name = "newUrlRequestBuilder")
    ),
    custom = { methodDef, _ ->
        val parameterTypes = methodDef.parameterTypes
        val parameterTypesSize = parameterTypes.size
        (parameterTypesSize == 6 || parameterTypesSize == 7 || parameterTypesSize == 8) &&
                parameterTypes[1] == "Ljava/util/Map;"
                && indexOfNewUrlRequestBuilderInstruction(methodDef) >= 0
    }
)

internal object CreateStreamingDataFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    parameters = listOf("L"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.IPUT_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.IF_NEZ,
        Opcode.SGET_OBJECT,
        Opcode.IPUT_OBJECT,
    ),
    custom = { _, classDef ->
        classDef.fields.any { field ->
            field.name == "a" && field.type.endsWith($$"/StreamingDataOuterClass$StreamingData;")
        }
    }
)

internal object BuildMediaDataSourceFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
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
        "Ljava/lang/Object;",
    )
)

internal object HlsCurrentTimeFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Z", "L"),
    filters = listOf(
        literal(45355374L)
    )
)

internal object MediaFetchEnumConstructorFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf(
        "ENABLED",
        "DISABLED_FOR_PLAYBACK",
        "DISABLED_BY_SABR_STREAMING_URI"
    )
)

internal object NerdsStatsVideoFormatBuilderFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Ljava/lang/String;",
    parameters = listOf("L"),
    filters = listOf(
        string("codecs=\"")
    )
)

val accountIdentityFingerprint = Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    strings = listOf(
        "Null getId",
        "Null getAccountName",
        "Null getPageId",
        "Null getDataSyncId",
        "Null getGaiaDelegationType",
        "Null getDelegationContext"
    ),
    custom = { method, _ ->
        val parameterTypes = method.parameterTypes
        parameterTypes.size > 4 && parameterTypes[2] == "Ljava/lang/String;" && parameterTypes[3] == "Z"
    }
)

internal object MediaFetchHotConfigFingerprint : Fingerprint(
    filters = listOf(
        literal(45645570L)
    )
)

internal object MediaFetchHotConfigAlternativeFingerprint : Fingerprint(
    filters = listOf(
        literal(45683169L)
    )
)

internal object PlaybackStartDescriptorFeatureFlagFingerprint : Fingerprint(
    filters = listOf(
        literal(45665455L)
    )
)

internal object MediaSessionFeatureFlagFingerprint : Fingerprint(
    parameters = listOf(),
    returnType = "Z",
    filters = listOf(
        literal(45640404L)
    )
)

internal object ReelItemWatchResponseFeatureFlagFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        literal(45638126L)
    )
)

internal fun indexOfNewUrlRequestBuilderInstruction(method: Method) = method.indexOfFirstInstruction {
    val reference = getReference<MethodReference>()
    opcode == Opcode.INVOKE_VIRTUAL && reference?.definingClass == "Lorg/chromium/net/CronetEngine;"
            && reference.name == "newUrlRequestBuilder"
            && reference.parameterTypes.size == 3
            && reference.parameterTypes[0] == "Ljava/lang/String;"
            && reference.parameterTypes[1] == $$"Lorg/chromium/net/UrlRequest$Callback;"
            && reference.parameterTypes[2] == "Ljava/util/concurrent/Executor;"
}
