package app.revanced.patches.youtube.misc.oldlayout.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.Opcode

object VersionOverrideFingerprint : MethodFingerprint(
    opcodes = listOf(
        Opcode.IF_EQZ,
        Opcode.CONST_STRING
    ),
    customFingerprint = {
        it.definingClass.endsWith("VersionOverridePatch;") && it.name == "getVersionOverride"
    }
)
