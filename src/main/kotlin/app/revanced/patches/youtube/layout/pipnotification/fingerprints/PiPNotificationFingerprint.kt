package app.revanced.patches.youtube.layout.pipnotification.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

object PiPNotificationFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.CHECK_CAST,
        Opcode.IGET_OBJECT,
        Opcode.IF_EQZ,
        Opcode.INVOKE_VIRTUAL
    ),
    strings = listOf("honeycomb.Shell\$HomeActivity")
)