package app.revanced.patches.youtube.misc.timebar.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch
import org.jf.dexlib2.iface.instruction.WideLiteralInstruction
import org.jf.dexlib2.Opcode

object EmptyColorFingerprint : MethodFingerprint(
    returnType = "V",
    opcodes = listOf(Opcode.DIV_LONG_2ADDR),
    customFingerprint = { methodDef ->
        methodDef.implementation?.instructions?.any {
            it.opcode.ordinal == Opcode.CONST.ordinal &&
            (it as? WideLiteralInstruction)?.wideLiteral == SharedResourceIdPatch.emptyColorLabelId
        } == true
    }
)