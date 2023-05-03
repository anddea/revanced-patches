package app.revanced.patches.youtube.misc.returnyoutubedislike.oldlayout.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode

object SlimMetadataButtonTextFingerprint : MethodFingerprint(
    returnType = "V",
    access = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf(),
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.IGET_OBJECT,
        Opcode.IGET
    )
)