package app.revanced.patches.youtube.general.categorybar.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.RelatedChipCloudMargin
import app.revanced.util.bytecode.isWideLiteralExists
import org.jf.dexlib2.Opcode

object RelatedChipCloudFingerprint : MethodFingerprint(
    opcodes = listOf(
        Opcode.CONST,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT
    ),
    customFingerprint = { methodDef, _ -> methodDef.isWideLiteralExists(RelatedChipCloudMargin) }
)