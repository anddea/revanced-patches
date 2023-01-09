package app.revanced.patches.youtube.layout.fullscreen.flimstripoverlay.bytecode.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourcdIdPatch
import org.jf.dexlib2.iface.instruction.WideLiteralInstruction
import org.jf.dexlib2.Opcode

object ScrubbingLabelFingerprint : MethodFingerprint(
    opcodes = listOf(Opcode.IPUT_BOOLEAN),
    customFingerprint = { methodDef ->
        methodDef.implementation?.instructions?.any {
            it.opcode.ordinal == Opcode.CONST.ordinal &&
            (it as? WideLiteralInstruction)?.wideLiteral == SharedResourcdIdPatch.scrubbingLabelId
        } == true
    }
)