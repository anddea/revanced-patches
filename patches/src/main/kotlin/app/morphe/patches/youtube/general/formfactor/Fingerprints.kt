package app.morphe.patches.youtube.general.formfactor

import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val formFactorEnumConstructorFingerprint = legacyFingerprint(
    name = "formFactorEnumConstructorFingerprint",
    returnType = "V",
    strings = listOf(
        "UNKNOWN_FORM_FACTOR",
        "SMALL_FORM_FACTOR",
        "LARGE_FORM_FACTOR",
        "AUTOMOTIVE_FORM_FACTOR",
    )
)

internal val widthDpUIFingerprint = legacyFingerprint(
    name = "widthDpUIFingerprint",
    returnType = "I",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    opcodes = listOf(
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
        Opcode.RETURN,
    ),
    literals = listOf(
        480L,
        600L,
        720L
    )
)