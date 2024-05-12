package app.revanced.patches.youtube.general.toolbar.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.util.fingerprint.MethodReferenceNameFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object ActionBarRingoTextFingerprint : MethodReferenceNameFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    opcodes = listOf(
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_STATIC
    ),
    reference = { "setStartDelay" }
)
