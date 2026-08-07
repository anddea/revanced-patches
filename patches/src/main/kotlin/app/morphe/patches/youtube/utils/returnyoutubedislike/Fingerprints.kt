package app.morphe.patches.youtube.utils.returnyoutubedislike

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchFirst
import app.morphe.patcher.OpcodesFilter
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.string
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

internal object DislikeFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(
        string("like/dislike"),
    ),
)

internal object EndpointServiceNameFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PROTECTED, AccessFlags.FINAL),
    parameters = listOf(),
    returnType = "L",
    filters = listOf(
        string("serviceName"),
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            definingClass = "this",
            type = "Ljava/lang/String;",
        ),
    ),
)

internal fun likeEndpointParserFingerprint(definingClass: String) = object : Fingerprint(
    definingClass = definingClass,
    returnType = "V",
    filters = listOf(
        fieldAccess(
            opcode = Opcode.SGET_OBJECT,
            location = MatchFirst(),
        ),
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            definingClass = "this",
        ),
        string(""),
    ),
) {}

internal fun requestParameterCheckFingerprint(definingClass: String) = object : Fingerprint(
    definingClass = definingClass,
    accessFlags = listOf(AccessFlags.PROTECTED, AccessFlags.FINAL),
    parameters = listOf(),
    filters = listOf(
        // playlistId
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            type = "Ljava/lang/String;",
        ),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            smali = "Ljava/lang/String;->isEmpty()Z",
        ),
        // videoId
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            type = "Ljava/lang/String;",
        ),
    ),
) {}

/**
 * This fingerprint is compatible with YouTube v18.30.xx+
 */
internal object RollingNumberMeasureAnimatedTextFingerprint : Fingerprint(
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.ADD_FLOAT_2ADDR, // measuredTextWidth
        Opcode.ADD_INT_LIT8,
        Opcode.GOTO,
    ),
    custom = { method, _ ->
        method.indexOfFirstInstruction {
            getReference<MethodReference>()?.toString() == "Landroid/text/TextPaint;->measureText([CII)F"
        } >= 0
    },
)

internal object RollingNumberMeasureStaticLabelFingerprint : Fingerprint(
    returnType = "F",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf("Ljava/lang/String;"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.RETURN,
    ),
)

internal object RollingNumberMeasureTextParentFingerprint : Fingerprint(
    returnType = "Ljava/lang/String;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    parameters = listOf(),
    filters = listOf(
        string("RollingNumberFontProperties{paint="),
    ),
)

/**
 * This fingerprint is compatible with YouTube v18.29.38+
 */
internal object RollingNumberSetterFingerprint : Fingerprint(
    filters = OpcodesFilter.opcodesToFilters(Opcode.CHECK_CAST),
    custom = { method, _ ->
        method.indexOfFirstInstruction {
            opcode == Opcode.CONST_STRING &&
                    getReference<StringReference>()
                        ?.string.toString()
                        .startsWith("RollingNumberType required properties missing! Need")
        } >= 0
    },
)

internal const val LITHO_NEW_TEXT_COMPONENT_FEATURE_FLAG = 45675738L

internal object TextComponentFeatureFlagFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.FINAL),
    returnType = "Z",
    parameters = emptyList(),
    filters = listOf(
        literal(LITHO_NEW_TEXT_COMPONENT_FEATURE_FLAG),
    ),
)
