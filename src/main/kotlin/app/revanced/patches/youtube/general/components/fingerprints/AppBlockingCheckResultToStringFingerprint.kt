package app.revanced.patches.youtube.general.components.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

internal object AppBlockingCheckResultToStringFingerprint : MethodFingerprint(
    returnType = "Ljava/lang/String;",
    strings = listOf("AppBlockingCheckResult{intent=")
)
