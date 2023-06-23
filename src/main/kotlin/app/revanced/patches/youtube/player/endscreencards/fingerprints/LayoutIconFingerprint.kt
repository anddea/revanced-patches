package app.revanced.patches.youtube.player.endscreencards.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.EndScreenElementLayoutIcon
import app.revanced.util.bytecode.isWideLiteralExists
import org.jf.dexlib2.Opcode

object LayoutIconFingerprint : MethodFingerprint(
    opcodes = listOf(
        Opcode.CONST_4,
        Opcode.CONST,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CHECK_CAST,
    ),
    customFingerprint = { methodDef, _ -> methodDef.isWideLiteralExists(EndScreenElementLayoutIcon) }
)