package app.revanced.patches.youtube.utils.fix.clientspoof.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.Opcode

internal object SetPlayerRequestClientTypeFingerprint : MethodFingerprint(
    strings = listOf("10.29"),
    opcodes = listOf(
        Opcode.IGET,
        Opcode.IPUT, // Sets ClientInfo.clientId.
    ),
)