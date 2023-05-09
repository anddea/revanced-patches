package app.revanced.patches.music.layout.floatingbutton.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.util.bytecode.isNarrowLiteralExists
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode

object FloatingButtonParentFingerprint : MethodFingerprint(
    returnType = "V",
    access = AccessFlags.PROTECTED or AccessFlags.FINAL,
    parameters = listOf("L"),
    opcodes = listOf(Opcode.INVOKE_DIRECT),
    customFingerprint = { it.isNarrowLiteralExists(259982244) }
)

