package app.revanced.patches.youtube.layout.general.categorybar.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch.Companion.relatedChipCloudMarginId
import app.revanced.util.bytecode.isWideLiteralExists
import org.jf.dexlib2.Opcode

object RelatedChipCloudFingerprint : MethodFingerprint(
    opcodes = listOf(
        Opcode.CONST,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT
    ),
    customFingerprint = { it.isWideLiteralExists(relatedChipCloudMarginId) }
)