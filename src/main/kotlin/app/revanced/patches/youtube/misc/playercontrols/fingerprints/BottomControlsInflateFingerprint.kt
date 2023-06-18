package app.revanced.patches.youtube.misc.playercontrols.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch.Companion.bottomUiContainerResourceId
import app.revanced.util.bytecode.isWideLiteralExists
import org.jf.dexlib2.Opcode

object BottomControlsInflateFingerprint : MethodFingerprint(
    opcodes = listOf(
        Opcode.CHECK_CAST,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT
    ),
    customFingerprint = { it, _ -> it.isWideLiteralExists(bottomUiContainerResourceId) }
)