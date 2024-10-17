package app.revanced.patches.youtube.general.toolbar.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionReversed
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

object SearchBarFingerprint : MethodFingerprint(
    returnType = "V",
    parameters = listOf("Ljava/lang/String;"),
    opcodes = listOf(
        Opcode.IGET_OBJECT,
        Opcode.IF_EQZ,
        Opcode.IGET_BOOLEAN,
        Opcode.IF_EQZ
    ),
    customFingerprint = { methodDef, _ ->
        methodDef.indexOfFirstInstructionReversed {
            getReference<MethodReference>()?.name == "isEmpty"
        } >= 0
    }
)