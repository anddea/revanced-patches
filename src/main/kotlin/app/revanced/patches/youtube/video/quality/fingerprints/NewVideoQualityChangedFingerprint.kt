package app.revanced.patches.youtube.video.quality.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode

object NewVideoQualityChangedFingerprint : MethodFingerprint(
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.IGET,
        Opcode.CONST_4,
        Opcode.IF_NE,
        Opcode.NEW_INSTANCE,
        Opcode.IGET_OBJECT,
        Opcode.CHECK_CAST,
        Opcode.IGET
    )
)
