package app.revanced.patches.youtube.misc.playerbutton.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch.Companion.liveChatButtonId
import app.revanced.util.bytecode.isWideLiteralExists
import org.jf.dexlib2.Opcode

object LiveChatFingerprint : MethodFingerprint(
    opcodes = listOf(Opcode.NEW_INSTANCE),
    customFingerprint = { it.isWideLiteralExists(liveChatButtonId) }
)