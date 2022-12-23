package app.revanced.shared.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode

object OnDrawFingerprint : MethodFingerprint (
    "V", AccessFlags.PUBLIC or AccessFlags.FINAL, listOf("L"),
    listOf(Opcode.MOVE_OBJECT_FROM16, Opcode.MOVE_OBJECT_FROM16),
    customFingerprint = { methodDef ->
        methodDef.name == "onDraw"
    }
)