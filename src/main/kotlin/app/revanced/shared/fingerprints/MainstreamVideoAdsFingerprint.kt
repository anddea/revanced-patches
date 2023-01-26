package app.revanced.shared.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode

object MainstreamVideoAdsFingerprint : MethodFingerprint(
    returnType = "V",
    access = AccessFlags.PRIVATE or AccessFlags.FINAL,
    parameters = listOf("L","Z"),
    opcodes = listOf(
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CONST_4,
        Opcode.IPUT_BOOLEAN,
        Opcode.IF_NEZ
    )
)