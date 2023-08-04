package app.revanced.patches.youtube.utils.litho.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import com.android.tools.smali.dexlib2.Opcode

object GeneralByteBufferFingerprint : MethodFingerprint(
    returnType = "L",
    opcodes = listOf(
        Opcode.ADD_INT_2ADDR,
        Opcode.INVOKE_VIRTUAL
    ),
    strings = listOf("Unssuported TextDecorator adjustment. Extension: %s")
)