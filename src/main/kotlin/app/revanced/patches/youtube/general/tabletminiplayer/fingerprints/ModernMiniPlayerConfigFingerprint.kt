package app.revanced.patches.youtube.general.tabletminiplayer.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.util.fingerprint.MethodReferenceNameFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object ModernMiniPlayerConfigFingerprint : MethodReferenceNameFingerprint(
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    parameters = listOf("Landroid/content/Context;", "L", "L", "L", "L", "L", "L"),
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.IF_EQZ
    ),
    reference = { "getClass" }
)