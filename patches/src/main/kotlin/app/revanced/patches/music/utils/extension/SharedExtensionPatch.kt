package app.revanced.patches.music.utils.extension

import app.revanced.patches.music.utils.extension.hooks.applicationInitHook
import app.revanced.patches.shared.extension.sharedExtensionPatch

val sharedExtensionPatch = sharedExtensionPatch(applicationInitHook)
