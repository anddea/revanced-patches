package app.revanced.patches.youtube.utils.quickactions.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.QuickActionsElementContainer
import app.revanced.util.bytecode.isWideLiteralExists
import com.android.tools.smali.dexlib2.Opcode

object QuickActionsElementFingerprint : MethodFingerprint(
    returnType = "V",
    opcodes = listOf(
        Opcode.RETURN_VOID,
        Opcode.IGET_OBJECT,
        Opcode.CONST,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CHECK_CAST,
        Opcode.CONST,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CHECK_CAST
    ),
    customFingerprint = { methodDef, _ -> methodDef.isWideLiteralExists(QuickActionsElementContainer) }
)