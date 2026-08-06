package app.morphe.patches.music.general.audiotracks

import app.morphe.patches.music.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE_MUSIC
import app.morphe.patches.music.utils.patch.PatchList.FORCE_ORIGINAL_AUDIO
import app.morphe.patches.music.utils.playservice.is_8_05_or_greater
import app.morphe.patches.music.utils.playservice.is_9_26_or_greater
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
            "morphe_force_original_audio",
            "true"
        )

        updatePatchStatus(FORCE_ORIGINAL_AUDIO)
    },
    fixUseLocalizedAudioTrackFlag = { is_8_05_or_greater && !is_9_26_or_greater },
    forcedServerAdaptiveStreaming = { is_9_26_or_greater }
)
