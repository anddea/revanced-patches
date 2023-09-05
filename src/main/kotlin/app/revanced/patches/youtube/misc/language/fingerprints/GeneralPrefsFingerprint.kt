package app.revanced.patches.youtube.misc.language.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import com.android.tools.smali.dexlib2.Opcode

object GeneralPrefsFingerprint : MethodFingerprint(
    returnType = "V",
    parameters = emptyList(),
    opcodes = listOf(
        Opcode.MOVE_RESULT,
        Opcode.IF_NEZ,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.GOTO
    ),
    strings = listOf("bedtime_reminder_toggle"),
    customFingerprint = { methodDef, _ -> methodDef.definingClass.endsWith("/GeneralPrefsFragment;") }
)