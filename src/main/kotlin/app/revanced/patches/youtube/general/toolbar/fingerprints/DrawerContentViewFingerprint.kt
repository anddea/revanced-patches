package app.revanced.patches.youtube.general.toolbar.fingerprints

import app.revanced.util.fingerprint.MethodReferenceNameFingerprint
import com.android.tools.smali.dexlib2.Opcode

internal object DrawerContentViewFingerprint : MethodReferenceNameFingerprint(
    returnType = "V",
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.NEW_INSTANCE,
        Opcode.INVOKE_DIRECT,
    ),
    reference = { "addView" }
)
