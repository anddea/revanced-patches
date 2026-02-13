package app.morphe.patches.shared.webview

import app.morphe.util.fingerprint.legacyFingerprint

internal val webViewHostActivityOnCreateFingerprint = legacyFingerprint(
    name = "webViewHostActivityOnCreateFingerprint",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
    customFingerprint = { method, classDef ->
        classDef.endsWith("/WebViewHostActivity;") && method.name == "onCreate"
    }
)