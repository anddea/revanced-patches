package app.revanced.patches.youtube.misc.openlinksdirectly.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

object OpenLinksDirectlyFingerprintPrimary : MethodFingerprint(
    returnType = "Ljava/lang/Object",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Ljava/lang/Object"),
    opcodes = listOf(
        Opcode.CHECK_CAST,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.RETURN_OBJECT
    ),
    customFingerprint = custom@{ methodDef, _ ->
        if (methodDef.implementation == null)
            return@custom false
        if (methodDef.name != "a")
            return@custom false

        var count = 0
        for (instruction in methodDef.implementation!!.instructions) {
            if (instruction.opcode != Opcode.SGET_OBJECT)
                continue

            val objectInstruction = instruction as ReferenceInstruction
            if ((objectInstruction.reference as FieldReference).name != "webviewEndpoint")
                continue

            count++
        }
        count == 1
    }
)