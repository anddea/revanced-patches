package app.revanced.patches.youtube.misc.returnyoutubedislike.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.Opcode

object TextComponentSpecFingerprint : MethodFingerprint(
    returnType = "L",
    opcodes = listOf(
        Opcode.INVOKE_STATIC,
        Opcode.RETURN_OBJECT, // last instruction of the method
    ),
    strings = listOf("TextComponentSpec: No converter for extension: %s")
)