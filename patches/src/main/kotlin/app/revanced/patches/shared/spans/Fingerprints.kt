package app.revanced.patches.shared.spans

import app.revanced.util.fingerprint.legacyFingerprint

internal val customCharacterStyleFingerprint = legacyFingerprint(
    name = "customCharacterStyleFingerprint",
    returnType = "Landroid/graphics/Path;",
    parameters = listOf("Landroid/text/Layout;"),
)

