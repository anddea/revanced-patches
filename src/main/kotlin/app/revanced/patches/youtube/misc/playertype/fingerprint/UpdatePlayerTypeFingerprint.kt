package app.revanced.patches.youtube.misc.playertype.fingerprint

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode

object UpdatePlayerTypeFingerprint : MethodFingerprint(
    returnType = "V",
    access = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.IF_NE,
        Opcode.RETURN_VOID
    ),
    customFingerprint = {
        it.definingClass.endsWith("YouTubePlayerOverlaysLayout;")
    }
)
