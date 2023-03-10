package app.revanced.patches.youtube.misc.playerbutton.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch
import org.jf.dexlib2.iface.instruction.WideLiteralInstruction
import org.jf.dexlib2.Opcode

object LiveChatFingerprint : MethodFingerprint(
    opcodes = listOf(Opcode.NEW_INSTANCE),
    customFingerprint = { methodDef ->
        methodDef.implementation?.instructions?.any {
            it.opcode.ordinal == Opcode.CONST.ordinal &&
            (it as? WideLiteralInstruction)?.wideLiteral == SharedResourceIdPatch.liveChatButtonId
        } == true
    }
)