package app.revanced.patches.youtube.utils.webview

import app.revanced.util.fingerprint.legacyFingerprint

internal val webViewHostActivityOnCreateFingerprint = legacyFingerprint(
    name = "webViewHostActivityOnCreateFingerprint",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
    customFingerprint = { method, classDef ->
        classDef.endsWith("/WebViewHostActivity;") && method.name == "onCreate"
    }
)

internal val vrWelcomeActivityOnCreateFingerprint = legacyFingerprint(
    name = "vrWelcomeActivityOnCreateFingerprint",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
    customFingerprint = { method, classDef ->
        classDef.type == "Lcom/google/android/libraries/youtube/player/features/gl/vr/VrWelcomeActivity;"
                && method.name == "onCreate"
    }
)
