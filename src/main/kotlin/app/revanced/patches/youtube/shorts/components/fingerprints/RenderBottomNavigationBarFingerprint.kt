package app.revanced.patches.youtube.shorts.components.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.Opcode

internal object RenderBottomNavigationBarFingerprint : MethodFingerprint(
    returnType = "Landroid/view/View;",
    opcodes = listOf(
        Opcode.CONST_STRING,
        Opcode.INVOKE_VIRTUAL
    ),
    strings = listOf("r_pfcv")
)