package app.revanced.patches.youtube.general.audiotracks

import app.revanced.patches.shared.audiotracks.audioTracksPatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.patch.PatchList.DISABLE_FORCED_AUTO_AUDIO_TRACKS
import app.revanced.patches.youtube.utils.playservice.is_20_07_or_greater
import app.revanced.patches.youtube.utils.playservice.versionCheckPatch
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch

@Suppress("unused")
val audioTracksPatch = audioTracksPatch(
    block = {
        compatibleWith(COMPATIBLE_PACKAGE)

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
                "SETTINGS: DISABLE_AUTO_AUDIO_TRACKS"
            ),
            DISABLE_FORCED_AUTO_AUDIO_TRACKS
        )

        // endregion
    },
    fixUseLocalizedAudioTrackFlag = is_20_07_or_greater
)
