package app.revanced.patches.youtube.player.customspeedoverlay.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.Opcode

object YouTubeTextViewFingerprint : MethodFingerprint(
    returnType = "V",
    opcodes = listOf(Opcode.INVOKE_SUPER),
    customFingerprint = { it, _ -> it.definingClass.endsWith("YouTubeTextView;") && it.name == "setText" }
)