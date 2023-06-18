package app.revanced.patches.youtube.layout.navigation.tabletnavbar.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.Opcode

object PivotBarChangedFingerprint : MethodFingerprint(
    returnType = "V",
    opcodes = listOf(
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT
    ),
    customFingerprint = { it, _ -> it.definingClass.endsWith("PivotBar;") && it.name == "onConfigurationChanged" }
)