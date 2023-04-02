package app.revanced.patches.youtube.layout.navigation.shortsnavbar.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.Opcode

object ReelWatchBundleFingerprint : MethodFingerprint(
    returnType = "V",
    opcodes = listOf(
        Opcode.CHECK_CAST,
        Opcode.IGET_OBJECT,
        Opcode.CONST_STRING,
        Opcode.INVOKE_VIRTUAL
    ),
    strings = listOf("r_as")
)