package app.revanced.patches.youtube.misc.language.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.Opcode

object GeneralPrefsLegacyFingerprint : MethodFingerprint(
    returnType = "V",
    parameters = emptyList(),
    opcodes = listOf(
        Opcode.IGET_OBJECT,
        Opcode.CONST_WIDE_32,
        Opcode.CONST_4,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.IF_NEZ
    ),
    strings = listOf("bedtime_reminder_toggle"),
    customFingerprint = { methodDef, _ -> methodDef.definingClass.endsWith("/GeneralPrefsFragment;") }
)