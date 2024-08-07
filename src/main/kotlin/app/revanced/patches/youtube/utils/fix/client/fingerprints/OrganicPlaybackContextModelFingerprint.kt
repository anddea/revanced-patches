package app.revanced.patches.youtube.utils.fix.client.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object OrganicPlaybackContextModelFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    parameters = listOf(
        "Ljava/lang/String;", // cpn
        "Z",
        "Z",
        "Z",
        "Z"
    ),
    opcodes = listOf(
        Opcode.INVOKE_DIRECT,
        Opcode.IF_EQZ,
        Opcode.IPUT_OBJECT,
    ),
    strings = listOf("Null contentCpn")
)
