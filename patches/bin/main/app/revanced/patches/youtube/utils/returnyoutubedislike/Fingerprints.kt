package app.revanced.patches.youtube.utils.returnyoutubedislike

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

/**
 * This fingerprint is compatible with YouTube v18.30.xx+
 */
internal val rollingNumberMeasureAnimatedTextFingerprint = legacyFingerprint(
    name = "rollingNumberMeasureAnimatedTextFingerprint",
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.ADD_FLOAT_2ADDR, // measuredTextWidth
        Opcode.ADD_INT_LIT8,
        Opcode.GOTO
    ),
    customFingerprint = { method, _ ->
        method.indexOfFirstInstruction {
            getReference<MethodReference>()?.toString() == "Landroid/text/TextPaint;->measureText([CII)F"
        } >= 0
    }
)

internal val rollingNumberMeasureStaticLabelFingerprint = legacyFingerprint(
    name = "rollingNumberMeasureStaticLabelFingerprint",
    returnType = "F",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Ljava/lang/String;"),
    opcodes = listOf(
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.RETURN
    )
)

internal val rollingNumberMeasureTextParentFingerprint = legacyFingerprint(
    name = "rollingNumberMeasureTextParentFingerprint",
    returnType = "Ljava/lang/String;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf(),
    strings = listOf("RollingNumberFontProperties{paint=")
)

/**
 * This fingerprint is compatible with YouTube v18.29.38+
 */
internal val rollingNumberSetterFingerprint = legacyFingerprint(
    name = "rollingNumberSetterFingerprint",
    opcodes = listOf(Opcode.CHECK_CAST),
    literals = listOf(45427773L),
)

internal val shortsTextViewFingerprint = legacyFingerprint(
    name = "shortsTextViewFingerprint",
    returnType = "V",
    parameters = listOf("L", "L"),
    opcodes = listOf(
        Opcode.IF_EQZ,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.GOTO,
        Opcode.IGET,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IF_NEZ,
        Opcode.SGET_OBJECT,
        Opcode.SGET_OBJECT,
        Opcode.IF_NE,
        Opcode.IGET,
        Opcode.AND_INT_LIT8,
        Opcode.IF_EQZ,
        Opcode.IGET_OBJECT,
        Opcode.IF_NEZ,
        Opcode.SGET_OBJECT,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.GOTO,
        Opcode.IGET,
        Opcode.AND_INT_LIT8,
        Opcode.IF_EQZ,
        Opcode.IGET_OBJECT,
        Opcode.IF_NEZ,
        Opcode.SGET_OBJECT,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.RETURN_VOID
    ),
    customFingerprint = { _, classDef ->
        classDef.methods.count() == 3
    }
)

