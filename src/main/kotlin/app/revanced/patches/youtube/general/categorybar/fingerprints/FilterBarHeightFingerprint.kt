package app.revanced.patches.youtube.general.categorybar.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.FilterBarHeight
import app.revanced.util.bytecode.isWideLiteralExists
import org.jf.dexlib2.Opcode

object FilterBarHeightFingerprint : MethodFingerprint(
    opcodes = listOf(
        Opcode.CONST,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.IPUT
    ),
    customFingerprint = { it, _ -> it.isWideLiteralExists(FilterBarHeight) }
)