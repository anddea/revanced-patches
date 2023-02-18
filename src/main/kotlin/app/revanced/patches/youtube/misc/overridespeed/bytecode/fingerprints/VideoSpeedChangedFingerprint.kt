package app.revanced.patches.youtube.misc.overridespeed.bytecode.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.Opcode

object VideoSpeedChangedFingerprint : MethodFingerprint(
    opcodes = listOf(
        Opcode.IGET_OBJECT,
        Opcode.IF_EQZ,
        Opcode.IF_EQZ,
        Opcode.IGET,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL
    ),
    customFingerprint = { it.name == "onItemClick" }
)