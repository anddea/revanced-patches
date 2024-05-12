package app.revanced.patches.youtube.utils.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

internal object TextComponentSpecFingerprint : MethodFingerprint(
    returnType = "Ljava/lang/CharSequence;",
    strings = listOf("Failed to set PB Style Run Extension in TextComponentSpec. Extension id: %s")
)