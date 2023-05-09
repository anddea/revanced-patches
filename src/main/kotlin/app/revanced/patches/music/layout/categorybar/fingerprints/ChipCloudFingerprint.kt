package app.revanced.patches.music.layout.categorybar.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.music.misc.resourceid.patch.SharedResourceIdPatch.Companion.chipCloudId
import app.revanced.util.bytecode.isWideLiteralExists
import org.jf.dexlib2.Opcode

object ChipCloudFingerprint : MethodFingerprint(
    returnType = "V",
    opcodes = listOf(
        Opcode.CONST,
        Opcode.CONST_4,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT
    ),
    customFingerprint = { it.isWideLiteralExists(chipCloudId) }
)

