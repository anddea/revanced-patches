package app.revanced.patches.youtube.utils.extension

import app.revanced.patches.shared.extension.sharedExtensionPatch
import app.revanced.patches.youtube.utils.extension.hooks.applicationInitHook
import app.revanced.patches.youtube.utils.extension.hooks.mainActivityBaseContextHook

// TODO: Move this to a "Hook.kt" file. Same for other extension hook patches.
val sharedExtensionPatch = sharedExtensionPatch(
    applicationInitHook,
    mainActivityBaseContextHook,
)
