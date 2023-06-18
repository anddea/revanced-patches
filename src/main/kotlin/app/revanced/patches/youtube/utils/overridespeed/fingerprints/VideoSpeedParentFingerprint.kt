package app.revanced.patches.youtube.utils.overridespeed.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode

object VideoSpeedParentFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    parameters = listOf("L", "L", "[L", "I"),
    opcodes = listOf(
        Opcode.ARRAY_LENGTH,
        Opcode.IF_GE,
        Opcode.AGET_OBJECT,
        Opcode.NEW_INSTANCE
    )
)