package app.revanced.patches.shared.spans.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

internal object CustomCharacterStyleFingerprint : MethodFingerprint(
    returnType = "Landroid/graphics/Path;",
    parameters = listOf("Landroid/text/Layout;"),
)