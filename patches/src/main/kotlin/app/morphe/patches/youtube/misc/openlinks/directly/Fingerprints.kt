package app.morphe.patches.youtube.misc.openlinks.directly

import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

internal val openLinksDirectlyFingerprintPrimary = legacyFingerprint(
    name = "openLinksDirectlyFingerprintPrimary",
    returnType = "Ljava/lang/Object",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Ljava/lang/Object"),
    opcodes = listOf(
        Opcode.CHECK_CAST,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.RETURN_OBJECT
    ),
    customFingerprint = { method, _ ->
        method.name == "a" &&
                method.implementation
                    ?.instructions
                    ?.withIndex()
                    ?.filter { (_, instruction) ->
                        val reference = (instruction as? ReferenceInstruction)?.reference
                        reference is FieldReference &&
                                instruction.opcode == Opcode.SGET_OBJECT &&
                                reference.name == "webviewEndpoint"
                    }
                    ?.map { (index, _) -> index }
                    ?.size == 1
    }
)

internal val openLinksDirectlyFingerprintSecondary = legacyFingerprint(
    name = "openLinksDirectlyFingerprintSecondary",
    returnType = "Landroid/net/Uri",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    parameters = listOf("Ljava/lang/String"),
    opcodes = listOf(
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT
    ),
    strings = listOf("://")
)
