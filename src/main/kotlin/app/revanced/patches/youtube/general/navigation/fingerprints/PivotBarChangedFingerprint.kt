package app.revanced.patches.youtube.general.navigation.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.Opcode

internal object PivotBarChangedFingerprint : MethodFingerprint(
    returnType = "V",
    opcodes = listOf(
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT
    ),
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass.endsWith("/PivotBar;")
                && methodDef.name == "onConfigurationChanged"
    }
)