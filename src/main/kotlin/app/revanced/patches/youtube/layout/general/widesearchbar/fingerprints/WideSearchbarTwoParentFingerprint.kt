package app.revanced.patches.youtube.layout.general.widesearchbar.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags

object WideSearchbarTwoParentFingerprint : MethodFingerprint(
    returnType = "L",
    access = AccessFlags.PUBLIC or AccessFlags.STATIC,
    strings = listOf(
        "Callback already registered.",
        "Failed to create SpotlightModeController."
    )
)