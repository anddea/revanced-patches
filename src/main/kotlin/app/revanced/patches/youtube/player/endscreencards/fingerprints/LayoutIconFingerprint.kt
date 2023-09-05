package app.revanced.patches.youtube.player.endscreencards.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.EndScreenElementLayoutIcon
import app.revanced.util.bytecode.isWideLiteralExists
import com.android.tools.smali.dexlib2.Opcode

object LayoutIconFingerprint : MethodFingerprint(
    returnType = "Landroid/view/View;",
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CHECK_CAST,
    ),
    customFingerprint = { methodDef, _ -> methodDef.isWideLiteralExists(EndScreenElementLayoutIcon) }
)