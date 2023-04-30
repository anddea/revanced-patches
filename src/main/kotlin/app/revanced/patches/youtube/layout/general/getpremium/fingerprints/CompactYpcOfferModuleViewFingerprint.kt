package app.revanced.patches.youtube.layout.general.getpremium.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.Opcode

object CompactYpcOfferModuleViewFingerprint : MethodFingerprint(
    opcodes = listOf(
        Opcode.ADD_INT_2ADDR,
        Opcode.ADD_INT_2ADDR,
        Opcode.INVOKE_VIRTUAL,
        Opcode.RETURN_VOID
    ),
    customFingerprint = { it.definingClass.endsWith("CompactYpcOfferModuleView;") && it.name == "onMeasure" }
)