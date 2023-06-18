package app.revanced.patches.youtube.general.categorybar.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.BarContainerHeight
import app.revanced.util.bytecode.isWideLiteralExists
import org.jf.dexlib2.Opcode

object SearchResultsChipBarFingerprint : MethodFingerprint(
    opcodes = listOf(
        Opcode.CONST,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT
    ),
    customFingerprint = { it, _ -> it.isWideLiteralExists(BarContainerHeight) }
)