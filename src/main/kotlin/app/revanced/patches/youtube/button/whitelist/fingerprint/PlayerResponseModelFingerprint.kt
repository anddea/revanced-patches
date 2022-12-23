package app.revanced.patches.youtube.button.whitelist.fingerprint

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags

object PlayerResponseModelFingerprint : MethodFingerprint(
    returnType = "Z",
    access = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("J", "L"),
    strings = listOf(
        "Attempting to seek during an ad",
        "currentPositionMs.",
    )
)

