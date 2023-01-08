package app.revanced.patches.youtube.layout.general.tabletminiplayer.bytecode.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode

object MiniPlayerOverrideNoContextFingerprint : MethodFingerprint(
    returnType = "Z",
    access = AccessFlags.PRIVATE or AccessFlags.FINAL,
    opcodes = listOf(Opcode.RETURN), // anchor to insert the instruction
)