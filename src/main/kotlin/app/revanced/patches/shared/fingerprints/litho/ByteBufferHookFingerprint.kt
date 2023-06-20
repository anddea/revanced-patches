package app.revanced.patches.shared.fingerprints.litho

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.Opcode

object ByteBufferHookFingerprint : MethodFingerprint(
    returnType = "L",
    opcodes = listOf(
        Opcode.ADD_INT_2ADDR,
        Opcode.INVOKE_VIRTUAL
    ),
    strings = listOf("Unssuported TextDecorator adjustment. Extension: %s")
)