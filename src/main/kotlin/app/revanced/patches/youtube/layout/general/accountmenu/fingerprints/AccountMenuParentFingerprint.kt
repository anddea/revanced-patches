package app.revanced.patches.youtube.layout.general.accountmenu.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch.Companion.compactLinkId
import app.revanced.util.bytecode.isWideLiteralExists
import org.jf.dexlib2.Opcode

object AccountMenuParentFingerprint : MethodFingerprint(
    opcodes = listOf(
        Opcode.CONST,
        Opcode.CONST_4,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT
    ),
    customFingerprint = { it.isWideLiteralExists(compactLinkId) }
)