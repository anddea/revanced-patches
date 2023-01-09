package app.revanced.patches.youtube.layout.flyoutpanel.oldqualitylayout.bytecode.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourcdIdPatch
import org.jf.dexlib2.iface.instruction.WideLiteralInstruction
import org.jf.dexlib2.Opcode

object QualityMenuViewInflateFingerprint : MethodFingerprint(
    opcodes = listOf(Opcode.INVOKE_SUPER),
    customFingerprint = { methodDef ->
        methodDef.implementation?.instructions?.any {
            it.opcode.ordinal == Opcode.CONST.ordinal &&
            (it as? WideLiteralInstruction)?.wideLiteral == SharedResourcdIdPatch.videoqualityfragmentLabelId
        } == true
    }
)