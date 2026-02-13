package app.morphe.patches.shared.customspeed

import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val arrayGeneratorFingerprint = legacyFingerprint(
    name = "arrayGeneratorFingerprint",
    returnType = "[L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    opcodes = listOf(
        Opcode.CONST_4,
        Opcode.NEW_ARRAY
    ),
    strings = listOf("0.0#")
)

internal val limiterFallBackFingerprint = legacyFingerprint(
    name = "limiterFallBackFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.CONST_HIGH16,
        Opcode.CONST_HIGH16,
        Opcode.INVOKE_STATIC
    ),
    strings = listOf("Playback rate: %f")
)

internal val limiterFingerprint = legacyFingerprint(
    name = "limiterFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("F"),
    opcodes = listOf(
        Opcode.CONST_HIGH16,
        Opcode.CONST_HIGH16,
        Opcode.INVOKE_STATIC,
    )
)
