package app.morphe.patches.music.general.audiotracks

import app.morphe.patches.music.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE_MUSIC
import app.morphe.patches.music.utils.patch.PatchList.DISABLE_FORCED_AUTO_AUDIO_TRACKS
import app.morphe.patches.music.utils.playservice.is_8_12_or_greater
import app.morphe.patches.music.utils.playservice.versionCheckPatch
import app.morphe.patches.music.utils.settings.CategoryType
import app.morphe.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.morphe.patches.music.utils.settings.addSwitchPreference
import app.morphe.patches.music.utils.settings.settingsPatch
import app.morphe.patches.shared.audiotracks.audioTracksPatch

@Suppress("unused")
val audioTracksPatch = audioTracksPatch(
    block = {
        compatibleWith(COMPATIBILITY_YOUTUBE_MUSIC)

        dependsOn(
            settingsPatch,
            versionCheckPatch,
        )
    },
    executeBlock = {
        addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_disable_auto_audio_tracks",
            "true"
        )

        updatePatchStatus(DISABLE_FORCED_AUTO_AUDIO_TRACKS)
    },
    fixUseLocalizedAudioTrackFlag = is_8_12_or_greater
)
