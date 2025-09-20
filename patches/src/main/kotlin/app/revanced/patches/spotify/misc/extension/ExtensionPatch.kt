package app.revanced.patches.spotify.misc.extension

import app.revanced.patches.shared.misc.extension.sharedExtensionPatchNamed

val sharedExtensionPatch = sharedExtensionPatchNamed(
    "spotify", 
    mainActivityOnCreateHook,
    loadOrbitLibraryHook
)
