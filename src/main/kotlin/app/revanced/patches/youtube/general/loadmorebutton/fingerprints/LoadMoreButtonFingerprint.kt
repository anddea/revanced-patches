package app.revanced.patches.youtube.general.loadmorebutton.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.ExpandButtonDown
import app.revanced.util.bytecode.isWideLiteralExists
import org.jf.dexlib2.Opcode

object LoadMoreButtonFingerprint : MethodFingerprint(
    opcodes = listOf(
        Opcode.CONST,
        Opcode.CONST_4,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT
    ),
    customFingerprint = { methodDef, _ -> methodDef.isWideLiteralExists(ExpandButtonDown) }
)