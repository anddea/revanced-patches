package app.revanced.patches.music.utils.audiotracks

import app.revanced.patches.music.utils.extension.sharedExtensionPatch
import app.revanced.patches.music.utils.playservice.is_8_12_or_greater
import app.revanced.patches.music.utils.playservice.versionCheckPatch
import app.revanced.patches.shared.audiotracks.audioTracksHookPatch

val audioTracksHookPatch = audioTracksHookPatch(
    block = {
        dependsOn(
            sharedExtensionPatch,
            versionCheckPatch,
        )
    },
    fixUseLocalizedAudioTrackFlag = is_8_12_or_greater,
)
