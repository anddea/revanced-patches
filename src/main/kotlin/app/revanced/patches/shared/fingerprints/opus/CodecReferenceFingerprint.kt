package app.revanced.patches.shared.fingerprints.opus

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode

object CodecReferenceFingerprint : MethodFingerprint(
    returnType = "J",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    opcodes = listOf(Opcode.INVOKE_SUPER),
    strings = listOf("itag")
)