package app.revanced.patches.music.layout.compactdialog.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.music.misc.resourceid.patch.SharedResourceIdPatch
import org.jf.dexlib2.iface.instruction.WideLiteralInstruction
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode

object DialogSolidFingerprint : MethodFingerprint(
    returnType = "V",
    access = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_STATIC
    ),
    customFingerprint = { methodDef ->
        methodDef.implementation?.instructions?.any {
            it.opcode.ordinal == Opcode.CONST.ordinal &&
            (it as? WideLiteralInstruction)?.wideLiteral == SharedResourceIdPatch.dialogSolidLabelId
        } == true
    }
)

