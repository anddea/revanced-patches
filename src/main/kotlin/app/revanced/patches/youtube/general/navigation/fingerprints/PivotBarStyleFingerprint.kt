package app.revanced.patches.youtube.general.navigation.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.Opcode

internal object PivotBarStyleFingerprint : MethodFingerprint(
    returnType = "V",
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT,
        Opcode.XOR_INT_2ADDR
    ),
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass.endsWith("/PivotBar;")
    }
)