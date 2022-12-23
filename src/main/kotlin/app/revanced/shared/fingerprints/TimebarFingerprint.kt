package app.revanced.shared.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags

object TimebarFingerprint : MethodFingerprint(
    "V",
    AccessFlags.PUBLIC or AccessFlags.FINAL,
    listOf("L"),
    customFingerprint = { methodDef ->
        methodDef.definingClass.endsWith("/TimeBar;")
                && methodDef.name.contains("draw")
    }
)
