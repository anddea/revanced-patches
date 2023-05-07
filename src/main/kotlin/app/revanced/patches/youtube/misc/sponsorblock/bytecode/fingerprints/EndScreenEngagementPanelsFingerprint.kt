package app.revanced.patches.youtube.misc.sponsorblock.bytecode.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.Opcode

object EndScreenEngagementPanelsFingerprint : MethodFingerprint(
    returnType = "V",
    opcodes = listOf(Opcode.CONST_WIDE_16),
    strings = listOf("ITEM_COUNT")
)
