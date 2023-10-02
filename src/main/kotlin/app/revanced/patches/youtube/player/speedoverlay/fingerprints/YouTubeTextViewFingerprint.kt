package app.revanced.patches.youtube.player.speedoverlay.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import com.android.tools.smali.dexlib2.Opcode

object YouTubeTextViewFingerprint : MethodFingerprint(
    returnType = "V",
    opcodes = listOf(Opcode.INVOKE_SUPER),
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass.endsWith("/YouTubeTextView;")
                && methodDef.name == "setText"
    }
)