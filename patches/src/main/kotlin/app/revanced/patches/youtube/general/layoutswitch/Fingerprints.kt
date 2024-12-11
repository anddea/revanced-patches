package app.revanced.patches.youtube.general.layoutswitch

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val formFactorEnumConstructorFingerprint = legacyFingerprint(
    name = "formFactorEnumConstructorFingerprint",
    returnType = "V",
    strings = listOf(
        "UNKNOWN_FORM_FACTOR",
        "SMALL_FORM_FACTOR",
        "LARGE_FORM_FACTOR"
    )
)

internal val layoutSwitchFingerprint = legacyFingerprint(
    name = "layoutSwitchFingerprint",
    returnType = "I",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT,
        Opcode.IF_NEZ,
        Opcode.CONST_4,
        Opcode.RETURN,
        Opcode.CONST_16,
        Opcode.IF_GE,
        Opcode.CONST_4,
        Opcode.RETURN,
        Opcode.CONST_16,
        Opcode.IF_GE,
        Opcode.CONST_4,
        Opcode.RETURN,
        Opcode.CONST_16,
        Opcode.IF_GE,
        Opcode.CONST_4,
        Opcode.RETURN,
        Opcode.CONST_4,
        Opcode.RETURN
    )
)