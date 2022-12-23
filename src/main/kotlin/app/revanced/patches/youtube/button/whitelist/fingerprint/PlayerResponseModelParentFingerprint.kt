package app.revanced.patches.youtube.button.whitelist.fingerprint

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags

object PlayerResponseModelParentFingerprint : MethodFingerprint(
    "V", AccessFlags.PUBLIC or AccessFlags.FINAL, listOf("L"),
    strings = listOf(
        "setMetadata may only be called once",
        "Person",
    )
)

