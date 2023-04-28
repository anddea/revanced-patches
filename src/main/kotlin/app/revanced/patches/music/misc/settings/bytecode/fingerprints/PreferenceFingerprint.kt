package app.revanced.patches.music.misc.settings.bytecode.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.Opcode

object PreferenceFingerprint : MethodFingerprint(
    returnType = "V",
    parameters = listOf("Z"),
    opcodes = listOf(
        Opcode.RETURN_VOID,
        Opcode.XOR_INT_LIT8,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.IF_NE,
        Opcode.RETURN_VOID,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_INTERFACE
    ),
    customFingerprint = { it.definingClass == "Landroidx/preference/Preference;" }
)