package app.revanced.patches.music.player.components.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object MiniPlayerDefaultViewVisibilityFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Landroid/view/View;", "F"),
    opcodes = listOf(
        Opcode.IGET_OBJECT,
        Opcode.SUB_FLOAT_2ADDR,
        Opcode.SGET_OBJECT,
        Opcode.INVOKE_VIRTUAL
    ),
    customFingerprint = { methodDef, classDef ->
        methodDef.name == "a"
                && classDef.methods.count() == 3
    }
)
