package app.morphe.patches.shared.tracking

import app.morphe.util.fingerprint.legacyFingerprint
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

/**
 * Copy URL from sharing panel
 */
internal val copyTextEndpointFingerprint = legacyFingerprint(
    name = "copyTextEndpointFingerprint",
    returnType = "V",
    parameters = listOf("L", "Ljava/util/Map;"),
    opcodes = listOf(
        Opcode.IGET_OBJECT,
        Opcode.CONST_STRING,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.IGET_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_INTERFACE,
        Opcode.RETURN_VOID
    ),
    strings = listOf("text/plain")
)

/**
 * Sharing panel
 */
internal val shareLinkFormatterFingerprint = legacyFingerprint(
    name = "shareLinkFormatterFingerprint",
    opcodes = listOf(
        Opcode.IGET_OBJECT,
        Opcode.CHECK_CAST,
        Opcode.GOTO,
        null,
        Opcode.INVOKE_VIRTUAL
    ),
    customFingerprint = custom@{ method, _ ->
        method.implementation
            ?.instructions
            ?.withIndex()
            ?.filter { (_, instruction) ->
                val reference = (instruction as? ReferenceInstruction)?.reference
                instruction.opcode == Opcode.SGET_OBJECT &&
                        reference is FieldReference &&
                        reference.name == "androidAppEndpoint"
            }
            ?.map { (index, _) -> index }
            ?.size == 2
    }
)

/**
 * Sharing panel of System
 */
internal val systemShareLinkFormatterFingerprint = legacyFingerprint(
    name = "systemShareLinkFormatterFingerprint",
    returnType = "V",
    parameters = listOf("L", "Ljava/util/Map;"),
    strings = listOf("YTShare_Logging_Share_Intent_Endpoint_Byte_Array")
)
