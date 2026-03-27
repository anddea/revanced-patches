package app.morphe.patches.shared.spans

import app.morphe.util.fingerprint.legacyFingerprint

internal val customCharacterStyleFingerprint = legacyFingerprint(
    name = "customCharacterStyleFingerprint",
    returnType = "Landroid/graphics/Path;",
    parameters = listOf("Landroid/text/Layout;"),
)

