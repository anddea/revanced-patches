package app.revanced.patches.youtube.player.customspeedoverlay.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.Opcode

object YouTubeTextViewFingerprint : MethodFingerprint(
    returnType = "V",
    opcodes = listOf(Opcode.INVOKE_SUPER),
    customFingerprint = { methodDef, _ -> methodDef.definingClass.endsWith("YouTubeTextView;") && methodDef.name == "setText" }
)