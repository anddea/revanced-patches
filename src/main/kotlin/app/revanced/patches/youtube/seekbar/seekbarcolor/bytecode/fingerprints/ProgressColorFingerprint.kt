package app.revanced.patches.youtube.seekbar.seekbarcolor.bytecode.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.Opcode

object ProgressColorFingerprint : MethodFingerprint(
    returnType = "V",
    parameters = listOf("I"),
    opcodes = listOf(Opcode.OR_INT_LIT8)
)