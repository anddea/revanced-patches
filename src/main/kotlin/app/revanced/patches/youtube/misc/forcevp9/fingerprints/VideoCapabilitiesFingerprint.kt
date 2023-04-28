package app.revanced.patches.youtube.misc.forcevp9.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.Opcode

object VideoCapabilitiesFingerprint : MethodFingerprint(
    returnType = "V",
    opcodes = listOf(
        Opcode.IPUT,
        Opcode.IPUT,
        Opcode.IPUT,
        Opcode.IPUT
    ),
    customFingerprint = { it.name == "<init>" }
)
