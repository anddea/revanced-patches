package app.revanced.patches.shared.litho

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val bufferUpbFeatureFlagFingerprint = legacyFingerprint(
    name = "bufferUpbFeatureFlagFingerprint",
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    parameters = listOf("L"),
    literals = listOf(45419603L),
)

internal val byteBufferFingerprint = legacyFingerprint(
    name = "byteBufferFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("I", "Ljava/nio/ByteBuffer;"),
    opcodes = listOf(
        null,
        Opcode.IF_EQZ,
        Opcode.IPUT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.SUB_INT_2ADDR,
        Opcode.IPUT,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.IPUT,
        Opcode.RETURN_VOID,
        Opcode.CONST_4,
        Opcode.IPUT,
        Opcode.IPUT,
        Opcode.GOTO
    ),
    // Check method count and field count to support both YouTube and YouTube Music
    customFingerprint = { _, classDef ->
        classDef.methods.count() > 6
                && classDef.fields.count() > 4
    },
)

internal val emptyComponentsFingerprint = legacyFingerprint(
    name = "emptyComponentsFingerprint",
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    opcodes = listOf(
        Opcode.INVOKE_INTERFACE,
        Opcode.INVOKE_STATIC_RANGE,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IGET_OBJECT
    ),
    strings = listOf("Error while converting %s"),
)

/**
 * Since YouTube v19.18.41 and YT Music 7.01.53, pathBuilder is being handled by a different Method.
 */
internal val pathBuilderFingerprint = legacyFingerprint(
    name = "pathBuilderFingerprint",
    returnType = "L",
    strings = listOf("Number of bits must be positive"),
)

internal val pathUpbFeatureFlagFingerprint = legacyFingerprint(
    name = "pathUpbFeatureFlagFingerprint",
    returnType = "Z",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    literals = listOf(45631264L),
)