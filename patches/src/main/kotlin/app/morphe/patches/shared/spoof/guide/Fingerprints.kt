package app.morphe.patches.shared.spoof.guide

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import app.morphe.patches.shared.CLIENT_INFO_CLASS_DESCRIPTOR
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object GuideEndpointConstructorFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    returnType = "V",
    strings = listOf("guide"),
)

internal object GuideEndpointRequestBodyFingerprint : Fingerprint(
    classFingerprint = GuideEndpointConstructorFingerprint,
    returnType = "V",
    accessFlags = listOf(AccessFlags.PROTECTED, AccessFlags.FINAL),
    parameters = emptyList(),
    filters = listOf(opcode(Opcode.RETURN_VOID)),
)

private object BuildClientContextBodyConstructorFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
    returnType = "V",
    filters = listOf(
        string("Android Wear"),
        opcode(Opcode.IF_EQZ),
        string("Android Automotive", location = MatchAfterImmediately()),
        string("Android"),
        fieldAccess(opcode = Opcode.IPUT_OBJECT, location = MatchAfterImmediately()),
    ),
)

internal object BuildClientContextBodyFingerprint : Fingerprint(
    classFingerprint = BuildClientContextBodyConstructorFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "L",
    parameters = emptyList(),
    filters = listOf(
        fieldAccess(opcode = Opcode.SGET, name = "SDK_INT"),
        fieldAccess(
            opcode = Opcode.IPUT_OBJECT,
            definingClass = CLIENT_INFO_CLASS_DESCRIPTOR,
            type = "Ljava/lang/String;",
        ),
        opcode(Opcode.OR_INT_LIT16),
    ),
)
