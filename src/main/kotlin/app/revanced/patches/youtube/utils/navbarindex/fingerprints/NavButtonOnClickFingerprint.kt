package app.revanced.patches.youtube.utils.navbarindex.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

object NavButtonOnClickFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Landroid/view/View;"),
    opcodes = listOf(
        Opcode.INVOKE_DIRECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.RETURN_VOID,
        Opcode.IGET_OBJECT,
        Opcode.CHECK_CAST,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.INVOKE_VIRTUAL,  // insert index
        Opcode.RETURN_VOID
    ),
    customFingerprint = custom@{ methodDef, classDef ->
        if (classDef.methods.count() != 3)
            return@custom false

        if (methodDef.name != "onClick")
            return@custom false

        val instructions = methodDef.implementation?.instructions!!

        if (instructions.count() < 20)
            return@custom false

        var count = 0
        for (instruction in instructions) {
            if (instruction.opcode != Opcode.INVOKE_VIRTUAL)
                continue

            val invokeInstruction = instruction as ReferenceInstruction
            if (invokeInstruction.reference.toString() != "Ljava/util/ArrayList;->indexOf(Ljava/lang/Object;)I")
                continue

            count++
        }
        count == 2
    }
)