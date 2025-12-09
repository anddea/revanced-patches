package app.revanced.patches.youtube.utils.webview

import app.revanced.patches.shared.webview.webViewPatch
import app.revanced.patches.youtube.utils.extension.sharedExtensionPatch

val webViewPatch = webViewPatch(
    block = {
        dependsOn(sharedExtensionPatch)
    },
    targetActivityFingerprint = vrWelcomeActivityOnCreateFingerprint,
)
