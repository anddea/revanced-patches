package app.revanced.patches.youtube.misc.customvideobuffer.bytecode.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode

object MaxBufferFingerprint : MethodFingerprint(
    returnType = "Z",
    access = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("J", "J", "F"),
    opcodes = listOf(
        Opcode.IGET_BOOLEAN,
        Opcode.CONST_WIDE_16,
        Opcode.IF_EQZ,
        Opcode.INVOKE_VIRTUAL
    )
)