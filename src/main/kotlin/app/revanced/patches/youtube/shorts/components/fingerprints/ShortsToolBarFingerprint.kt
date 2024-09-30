package app.revanced.patches.youtube.shorts.components.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object ShortsToolBarFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    opcodes = listOf(Opcode.IPUT_BOOLEAN),
    strings = listOf("Null topBarButtons"),
    customFingerprint = { methodDef, _ ->
        methodDef.parameterTypes.firstOrNull() == "Z"
    }
)