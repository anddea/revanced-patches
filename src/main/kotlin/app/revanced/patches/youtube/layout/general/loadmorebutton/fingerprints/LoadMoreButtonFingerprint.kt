package app.revanced.patches.youtube.layout.general.loadmorebutton.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch.Companion.expandButtonId
import app.revanced.util.bytecode.isWideLiteralExists
import org.jf.dexlib2.Opcode

object LoadMoreButtonFingerprint : MethodFingerprint(
    opcodes = listOf(
        Opcode.CONST,
        Opcode.CONST_4,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT
    ),
    customFingerprint = { it.isWideLiteralExists(expandButtonId) }
)