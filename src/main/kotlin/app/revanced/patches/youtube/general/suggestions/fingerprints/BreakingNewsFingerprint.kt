package app.revanced.patches.youtube.general.suggestions.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.HorizontalCardList
import app.revanced.util.bytecode.isWideLiteralExists
import org.jf.dexlib2.Opcode

object BreakingNewsFingerprint : MethodFingerprint(
    opcodes = listOf(
        Opcode.CONST,
        Opcode.CONST_4,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT
    ),
    customFingerprint = { methodDef, _ -> methodDef.isWideLiteralExists(HorizontalCardList) }
)