package app.revanced.patches.music.utils.extension

import app.revanced.patches.music.utils.extension.hooks.applicationInitHook
import app.revanced.patches.music.utils.extension.hooks.mainActivityBaseContextHook
import app.revanced.patches.shared.extension.hooks.cronetEngineContextHook
import app.revanced.patches.shared.extension.hooks.firebaseInitProviderContextHook
import app.revanced.patches.shared.extension.sharedExtensionPatch

val sharedExtensionPatch = sharedExtensionPatch(
    applicationInitHook,
    cronetEngineContextHook,
    firebaseInitProviderContextHook,
    mainActivityBaseContextHook,
)
