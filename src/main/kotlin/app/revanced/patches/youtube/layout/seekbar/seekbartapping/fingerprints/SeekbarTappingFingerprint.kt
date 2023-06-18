package app.revanced.patches.youtube.layout.seekbar.seekbartapping.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.util.bytecode.isNarrowLiteralExists
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode

object SeekbarTappingFingerprint : MethodFingerprint(
    returnType = "Z",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.NEW_INSTANCE,
        Opcode.INVOKE_DIRECT,
        Opcode.IPUT_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.RETURN
    ),
    customFingerprint = { it, _ -> it.name == "onTouchEvent" && it.isNarrowLiteralExists(2147483647) }
)