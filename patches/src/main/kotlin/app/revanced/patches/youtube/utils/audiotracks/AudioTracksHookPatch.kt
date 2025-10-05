package app.revanced.patches.youtube.utils.audiotracks

import app.revanced.patches.shared.audiotracks.audioTracksHookPatch
import app.revanced.patches.youtube.utils.extension.sharedExtensionPatch
import app.revanced.patches.youtube.utils.playservice.is_20_07_or_greater
import app.revanced.patches.youtube.utils.playservice.versionCheckPatch

val audioTracksHookPatch = audioTracksHookPatch(
    block = {
        dependsOn(
            sharedExtensionPatch,
            versionCheckPatch,
        )
    },
    fixUseLocalizedAudioTrackFlag = is_20_07_or_greater,
)
