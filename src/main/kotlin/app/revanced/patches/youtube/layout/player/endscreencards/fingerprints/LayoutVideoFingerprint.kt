package app.revanced.patches.youtube.layout.player.endscreencards.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch.Companion.layoutVideoId
import app.revanced.util.bytecode.isWideLiteralExists
import org.jf.dexlib2.Opcode

object LayoutVideoFingerprint : MethodFingerprint(
    opcodes = listOf(
        Opcode.CONST,
        Opcode.CONST_4,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CHECK_CAST,
    ),
    customFingerprint = { it.isWideLiteralExists(layoutVideoId) }
)