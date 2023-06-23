package app.revanced.patches.youtube.general.crowdfundingbox.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.DonationCompanion
import app.revanced.util.bytecode.isWideLiteralExists
import org.jf.dexlib2.Opcode

object CrowdfundingBoxFingerprint : MethodFingerprint(
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.IPUT_OBJECT
    ),
    customFingerprint = { methodDef, _ -> methodDef.isWideLiteralExists(DonationCompanion) }
)