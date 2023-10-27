package app.revanced.patches.youtube.player.speedoverlay.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.Opcode

object SpeedOverlayHookFingerprint : MethodFingerprint(
    returnType = "V",
    opcodes = listOf(
        Opcode.CMPL_FLOAT,
        Opcode.IF_GEZ,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL
    ),
    strings = listOf("Failed to easy seek haptics vibrate."),
    customFingerprint = { methodDef, _ -> methodDef.name == "run" }
)