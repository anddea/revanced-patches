package app.revanced.patches.youtube.button.whitelist.fingerprint

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode

object PrimaryInjectFingerprint : MethodFingerprint(
    returnType = "V",
    access = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf(),
    opcodes = listOf(
        Opcode.IF_NEZ,
        Opcode.CONST_STRING,
        Opcode.INVOKE_STATIC,
        Opcode.RETURN_VOID,
        Opcode.IGET_OBJECT
    ),
    strings = listOf(
        "play() called when the player wasn\'t loaded.",
    )
)

