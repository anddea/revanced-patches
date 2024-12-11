package app.revanced.patches.reddit.utils.extension

import app.revanced.patches.reddit.utils.extension.hooks.applicationInitHook
import app.revanced.patches.shared.extension.sharedExtensionPatch

val sharedExtensionPatch = sharedExtensionPatch(applicationInitHook)
