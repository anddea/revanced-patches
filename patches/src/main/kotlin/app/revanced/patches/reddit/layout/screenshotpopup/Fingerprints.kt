package app.revanced.patches.reddit.layout.screenshotpopup

import app.revanced.util.fingerprint.legacyFingerprint

internal val screenshotTakenBannerFingerprint = legacyFingerprint(
    name = "screenshotTakenBannerFingerprint",
    returnType = "V",
    parameters = listOf("Landroidx/compose/runtime/", "I"),
    customFingerprint = { method, classDef ->
        classDef.type.endsWith("\$ScreenshotTakenBannerKt\$lambda-1\$1;") &&
                method.name == "invoke"
    }
)
