package app.revanced.patches.youtube.layout.general.pivotbar.shortspivotbar.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode

object SetPivotBarFingerprint : MethodFingerprint(
    returnType = "V",
    access = AccessFlags.PRIVATE or AccessFlags.FINAL,
    parameters = listOf("Z"),
    opcodes = listOf(
        Opcode.CHECK_CAST,
        Opcode.IF_EQZ
    )
)