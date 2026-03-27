package app.morphe.patches.youtube.utils.webview

import app.morphe.patches.shared.webview.webViewPatch
import app.morphe.patches.youtube.utils.extension.sharedExtensionPatch

val webViewPatch = webViewPatch(
    block = {
        dependsOn(sharedExtensionPatch)
    },
    targetActivityFingerprint = vrWelcomeActivityOnCreateFingerprint,
)
