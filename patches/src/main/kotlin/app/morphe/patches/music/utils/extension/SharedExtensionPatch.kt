package app.morphe.patches.music.utils.extension

import app.morphe.patches.music.utils.extension.hooks.applicationInitHook
import app.morphe.patches.shared.extension.sharedExtensionPatch

val sharedExtensionPatch = sharedExtensionPatch(
    applicationInitHook,
)
