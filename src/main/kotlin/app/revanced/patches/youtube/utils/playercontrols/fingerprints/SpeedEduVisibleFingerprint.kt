package app.revanced.patches.youtube.utils.playercontrols.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.Opcode

object SpeedEduVisibleFingerprint : MethodFingerprint(
    returnType = "V",
    opcodes = listOf(Opcode.IPUT_BOOLEAN),
    customFingerprint = { methodDef, _ -> methodDef.name == "<init>" }
)