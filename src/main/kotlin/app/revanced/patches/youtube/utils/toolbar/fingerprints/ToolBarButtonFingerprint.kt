package app.revanced.patches.youtube.utils.toolbar.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.MenuItemView
import app.revanced.util.bytecode.isWideLiteralExists
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

object ToolBarButtonFingerprint : MethodFingerprint(
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Landroid/view/MenuItem;"),
    returnType = "V",
    opcodes = listOf(
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT,
        Opcode.IGET_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL
    ),
    customFingerprint = { methodDef, _ -> methodDef.isWideLiteralExists(MenuItemView) }
)