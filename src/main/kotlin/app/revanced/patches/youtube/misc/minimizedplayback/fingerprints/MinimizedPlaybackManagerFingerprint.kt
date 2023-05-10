package app.revanced.patches.youtube.misc.minimizedplayback.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.util.bytecode.isNarrowLiteralExists
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode

object MinimizedPlaybackManagerFingerprint : MethodFingerprint(
    returnType = "Z",
    access = AccessFlags.PUBLIC or AccessFlags.STATIC,
    parameters = listOf("L"),
    opcodes = listOf(Opcode.AND_INT_LIT16),
    customFingerprint = { it.isNarrowLiteralExists(64657230) }
)