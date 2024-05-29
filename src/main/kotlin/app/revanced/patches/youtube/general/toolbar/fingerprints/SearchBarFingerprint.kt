package app.revanced.patches.youtube.general.toolbar.fingerprints

import app.revanced.util.fingerprint.MethodReferenceNameFingerprint
import com.android.tools.smali.dexlib2.Opcode

object SearchBarFingerprint : MethodReferenceNameFingerprint(
    returnType = "V",
    parameters = listOf("Ljava/lang/String;"),
    opcodes = listOf(
        Opcode.IGET_OBJECT,
        Opcode.IF_EQZ,
        Opcode.IGET_BOOLEAN,
        Opcode.IF_EQZ
    ),
    reference = { "isEmpty" }
)