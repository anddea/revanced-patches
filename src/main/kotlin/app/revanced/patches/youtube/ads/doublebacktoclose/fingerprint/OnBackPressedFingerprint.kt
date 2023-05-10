package app.revanced.patches.youtube.ads.doublebacktoclose.fingerprint

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.Opcode

object OnBackPressedFingerprint : MethodFingerprint(
    opcodes = listOf(Opcode.RETURN_VOID),
    customFingerprint = { it.definingClass.endsWith("WatchWhileActivity;") && it.name == "onBackPressed" }
)
