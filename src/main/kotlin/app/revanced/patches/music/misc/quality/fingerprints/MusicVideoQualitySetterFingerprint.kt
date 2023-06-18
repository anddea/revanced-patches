package app.revanced.patches.music.misc.quality.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.Opcode

object MusicVideoQualitySetterFingerprint : MethodFingerprint(
    returnType = "V",
    opcodes = listOf(
        Opcode.CHECK_CAST,
        Opcode.IPUT_OBJECT
    ),
    customFingerprint = { it, _ -> it.name == "<init>" }
)