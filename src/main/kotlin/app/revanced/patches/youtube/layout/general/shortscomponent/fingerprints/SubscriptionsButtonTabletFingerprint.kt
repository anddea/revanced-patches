package app.revanced.patches.youtube.layout.general.shortscomponent.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.Opcode

object SubscriptionsButtonTabletFingerprint : MethodFingerprint(
    returnType = "V",
    parameters = listOf("L", "L", "Z"),
    opcodes = listOf(
        Opcode.INVOKE_STATIC,
        Opcode.IGET,
        Opcode.IF_EQZ
    )
)