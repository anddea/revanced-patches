package app.revanced.patches.youtube.overlaybutton.download.hook.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

object PlaylistOfflineDownloadOnClickFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Landroid/view/View;"),
    opcodes = listOf(
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT,
        Opcode.IF_EQZ,
        null, // Opcode.RETURN_VOID or Opcode.GOTO
        Opcode.IGET_OBJECT
    ),
    customFingerprint = { methodDef, classDef ->
        methodDef.name == "onClick"
            && classDef.methods.count() == 2
    }
)