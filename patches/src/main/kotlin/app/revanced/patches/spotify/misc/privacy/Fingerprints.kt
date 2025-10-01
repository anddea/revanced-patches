package app.revanced.patches.spotify.misc.privacy

import app.revanced.patcher.extensions.InstructionExtensions.instructionsOrNull
import app.revanced.patcher.fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference

val shareLinkFingerprint = fingerprint {
    accessFlags(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR)
    parameters(
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Ljava/lang/String;"
    )
    returns("V")

    custom { _, classDef ->
        val toStringMethod = classDef.methods.firstOrNull {
            it.name == "toString" && it.parameters.isEmpty() && it.returnType == "Ljava/lang/String;"
        } ?: return@custom false

        val toStringInstructions = toStringMethod.instructionsOrNull ?: return@custom false
        toStringInstructions.any { instruction ->
            instruction.opcode == Opcode.CONST_STRING &&
                    (instruction as? ReferenceInstruction)?.reference?.let { ref ->
                        (ref as? StringReference)?.string?.startsWith("ShareUrl(url=") == true
                    } == true
        }
    }
}
