package app.revanced.patches.youtube.layout.theme.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint

internal object LightThemeFingerprint : MethodFingerprint(
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass.endsWith("/ThemeUtils;")
                && methodDef.name == "getLightHexValue"
    },
)