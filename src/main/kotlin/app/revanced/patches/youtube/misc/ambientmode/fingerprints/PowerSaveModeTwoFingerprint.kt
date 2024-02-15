package app.revanced.patches.youtube.misc.ambientmode.fingerprints /** #C# Add START */

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

object PowerSaveModeTwoFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Ljava/lang/Object;"),
    opcodes = listOf(
		Opcode.INVOKE_VIRTUAL, // isPowerSaveMode
		Opcode.MOVE_RESULT,
		Opcode.INVOKE_STATIC,
		Opcode.MOVE_RESULT_OBJECT,
		Opcode.IGET_OBJECT,
		Opcode.CHECK_CAST,
		Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.RETURN_VOID
    ),
    customFingerprint = custom@{ methodDef, _ ->
        if (methodDef.name != "accept")
            return@custom false

        val instructions = methodDef.implementation?.instructions!!

        var count = 0
        for (instruction in instructions) {
            if (instruction.opcode != Opcode.INVOKE_VIRTUAL)
                continue

            val invokeInstruction = instruction as Instruction35c
            if ((invokeInstruction.reference as MethodReference).name != "isPowerSaveMode")
                continue

            count++
        }
        count == 1
    }
) /** #C# Add END */