package app.revanced.patches.music.utils.webview

import app.revanced.patches.shared.webview.webViewPatch
import app.revanced.patches.youtube.utils.extension.sharedExtensionPatch

val webViewPatch = webViewPatch(
    block = {
        dependsOn(sharedExtensionPatch)
    },
    targetActivityFingerprint = carAppPermissionActivityOnCreateFingerprint,
)
