package app.revanced.patches.youtube.player.hapticfeedback.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.Opcode

object SeekHapticsFingerprint : MethodFingerprint(
    returnType = "V",
    opcodes = listOf(
        Opcode.SGET,
        Opcode.CONST_16,
        Opcode.IF_LE
    ),
    strings = listOf("Failed to easy seek haptics vibrate."),
    customFingerprint = { it, _ -> it.name == "run" }
)
