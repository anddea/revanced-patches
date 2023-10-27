package app.revanced.patches.youtube.utils.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.Opcode

object OnBackPressedFingerprint : MethodFingerprint(
    opcodes = listOf(Opcode.RETURN_VOID),
    customFingerprint = { methodDef, _ -> methodDef.definingClass.endsWith("/WatchWhileActivity;") && methodDef.name == "onBackPressed" }
)
