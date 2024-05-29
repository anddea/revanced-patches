package app.revanced.patches.youtube.player.hapticfeedback.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.Opcode

internal object SeekHapticsFingerprint : MethodFingerprint(
    returnType = "V",
    opcodes = listOf(Opcode.SGET),
    strings = listOf("Failed to easy seek haptics vibrate."),
    customFingerprint = { methodDef, _ -> methodDef.name == "run" }
)
