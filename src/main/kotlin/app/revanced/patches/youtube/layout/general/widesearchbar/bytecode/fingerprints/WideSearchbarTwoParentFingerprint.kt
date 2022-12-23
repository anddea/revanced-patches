package app.revanced.patches.youtube.layout.general.widesearchbar.bytecode.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags

object WideSearchbarTwoParentFingerprint : MethodFingerprint(
    "L",
    AccessFlags.PUBLIC or AccessFlags.STATIC,
    strings = listOf(
        "Callback already registered.",
        "Failed to create SpotlightModeController."
    )
)