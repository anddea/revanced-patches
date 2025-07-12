package app.revanced.patches.youtube.general.audiotracks

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.utils.audiotracks.audioTracksHookPatch
import app.revanced.patches.youtube.utils.audiotracks.disableForcedAudioTracks
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.patch.PatchList.DISABLE_FORCED_AUTO_AUDIO_TRACKS
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch

@Suppress("unused")
val audioTracksPatch = bytecodePatch(
    DISABLE_FORCED_AUTO_AUDIO_TRACKS.title,
    DISABLE_FORCED_AUTO_AUDIO_TRACKS.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        audioTracksHookPatch,
    )

    execute {

        disableForcedAudioTracks()

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "SETTINGS: DISABLE_AUTO_AUDIO_TRACKS"
            ),
            DISABLE_FORCED_AUTO_AUDIO_TRACKS
        )

        // endregion

    }
}
