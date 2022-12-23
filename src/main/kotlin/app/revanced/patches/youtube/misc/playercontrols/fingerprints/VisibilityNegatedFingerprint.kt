package app.revanced.patches.youtube.misc.playercontrols.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode

object VisibilityNegatedFingerprint : MethodFingerprint(
    "V", AccessFlags.PUBLIC or AccessFlags.FINAL, listOf("Z"), opcodes = listOf(
        Opcode.IF_EQZ
    )
)