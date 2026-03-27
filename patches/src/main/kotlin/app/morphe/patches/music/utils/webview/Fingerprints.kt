package app.morphe.patches.music.utils.webview

import app.morphe.util.fingerprint.legacyFingerprint

internal val carAppPermissionActivityOnCreateFingerprint = legacyFingerprint(
    name = "carAppPermissionActivityOnCreateFingerprint",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
    customFingerprint = { method, classDef ->
        classDef.type == "Landroidx/car/app/CarAppPermissionActivity;"
                && method.name == "onCreate"
    }
)
