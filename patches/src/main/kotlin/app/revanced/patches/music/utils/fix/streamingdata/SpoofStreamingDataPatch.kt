package app.revanced.patches.music.utils.fix.streamingdata

import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.compatibility.Constants.YOUTUBE_MUSIC_PACKAGE_NAME
import app.revanced.patches.music.utils.patch.PatchList.SPOOF_STREAMING_DATA
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.revanced.patches.music.utils.settings.addPreferenceWithIntent
import app.revanced.patches.music.utils.settings.addSwitchPreference
import app.revanced.patches.music.utils.settings.settingsPatch
import app.revanced.patches.shared.spoof.streamingdata.baseSpoofStreamingDataPatch
import app.revanced.patches.shared.spoof.useragent.baseSpoofUserAgentPatch

@Suppress("unused")
val spoofStreamingDataPatch = baseSpoofStreamingDataPatch(
    {
        compatibleWith(COMPATIBLE_PACKAGE)

        dependsOn(
            baseSpoofUserAgentPatch(YOUTUBE_MUSIC_PACKAGE_NAME),
            settingsPatch,
        )
    },
    {
        addSwitchPreference(
            CategoryType.MISC,
            "revanced_spoof_streaming_data",
            "true"
        )
        addPreferenceWithIntent(
            CategoryType.MISC,
            "revanced_spoof_streaming_data_type",
            "revanced_spoof_streaming_data"
        )
        addSwitchPreference(
            CategoryType.MISC,
            "revanced_spoof_streaming_data_stats_for_nerds",
            "true",
            "revanced_spoof_streaming_data"
        )

        updatePatchStatus(SPOOF_STREAMING_DATA)

    }
)
