package app.revanced.patches.youtube.utils.settings

import app.revanced.patches.youtube.utils.resourceid.appearance
import app.revanced.util.fingerprint.legacyFingerprint

internal val themeSetterSystemFingerprint = legacyFingerprint(
    name = "themeSetterSystemFingerprint",
    returnType = "L",
    literals = listOf(appearance),
)
