package app.revanced.patches.youtube.layout.seekbar.seekbarcolor.bytecode.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch
import org.jf.dexlib2.Opcode
import org.jf.dexlib2.iface.instruction.WideLiteralInstruction

object SeekbarColorFingerprint : MethodFingerprint(
    customFingerprint = { methodDef ->
        methodDef.implementation?.instructions?.any {
            it.opcode.ordinal == Opcode.CONST.ordinal &&
            (it as? WideLiteralInstruction)?.wideLiteral == SharedResourceIdPatch.inlineTimeBarColorizedBarPlayedColorDarkId
        } == true
    }
)