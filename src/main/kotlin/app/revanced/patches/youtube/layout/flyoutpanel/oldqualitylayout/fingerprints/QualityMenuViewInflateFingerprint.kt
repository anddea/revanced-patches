package app.revanced.patches.youtube.layout.flyoutpanel.oldqualitylayout.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch.Companion.videoQualityFragmentId
import app.revanced.util.bytecode.isWideLiteralExists
import org.jf.dexlib2.Opcode

object QualityMenuViewInflateFingerprint : MethodFingerprint(
    opcodes = listOf(
        Opcode.INVOKE_SUPER,
        Opcode.CONST,
        Opcode.CONST_4,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CONST,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CONST_16,
        Opcode.INVOKE_VIRTUAL,
        Opcode.CONST,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CHECK_CAST
    ),
    customFingerprint = { it.isWideLiteralExists(videoQualityFragmentId) }
)