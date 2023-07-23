package app.revanced.patches.youtube.misc.forcevp9.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

object Vp9SecondaryFingerprint : MethodFingerprint(
    returnType = "Z",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    parameters = listOf("L", "I"),
    opcodes = listOf(
        Opcode.RETURN,
        Opcode.CONST_4,
        Opcode.RETURN
    )
)
