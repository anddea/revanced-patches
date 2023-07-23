package app.revanced.patches.youtube.navigation.tabletnavbar.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import com.android.tools.smali.dexlib2.Opcode

object PivotBarChangedFingerprint : MethodFingerprint(
    returnType = "V",
    opcodes = listOf(
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT
    ),
    customFingerprint = { methodDef, _ -> methodDef.definingClass.endsWith("/PivotBar;") && methodDef.name == "onConfigurationChanged" }
)