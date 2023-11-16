package app.revanced.patches.youtube.utils.navbarindex.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

object DefaultTabsBarFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PROTECTED or AccessFlags.FINAL,
    parameters = listOf("Landroid/view/View;"),
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.IGET
    ),
    customFingerprint = { methodDef, _ -> methodDef.definingClass.endsWith("/DefaultTabsBar;") }
)