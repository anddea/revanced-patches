package app.morphe.patches.youtube.general.audiotracks

import app.morphe.patches.shared.audiotracks.audioTracksPatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.utils.patch.PatchList.FORCE_ORIGINAL_AUDIO
import app.morphe.patches.youtube.utils.playservice.is_20_07_or_greater
import app.morphe.patches.youtube.utils.playservice.is_21_26_or_greater
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch

@Suppress("unused")
val audioTracksPatch = audioTracksPatch(
    block = {
        compatibleWith(COMPATIBILITY_YOUTUBE)

        dependsOn(
            settingsPatch,
            versionCheckPatch,
        )
    },
    executeBlock = {
        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "SETTINGS: FORCE_ORIGINAL_AUDIO"
            ),
            FORCE_ORIGINAL_AUDIO
        )

        // endregion
    },
    fixUseLocalizedAudioTrackFlag = { is_20_07_or_greater && !is_21_26_or_greater },
    forcedServerAdaptiveStreaming = { is_21_26_or_greater }
)
