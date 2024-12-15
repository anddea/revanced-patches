package app.revanced.patches.youtube.utils.fix.streamingdata

import app.revanced.patches.shared.spoof.streamingdata.baseSpoofStreamingDataPatch
import app.revanced.patches.shared.spoof.useragent.baseSpoofUserAgentPatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.compatibility.Constants.YOUTUBE_PACKAGE_NAME
import app.revanced.patches.youtube.utils.patch.PatchList.SPOOF_STREAMING_DATA
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch

val spoofStreamingDataPatch = baseSpoofStreamingDataPatch(
    {
        compatibleWith(COMPATIBLE_PACKAGE)

        dependsOn(
            baseSpoofUserAgentPatch(YOUTUBE_PACKAGE_NAME),
            settingsPatch
        )
    },
    {
        addPreference(
            arrayOf(
                "SETTINGS: SPOOF_STREAMING_DATA"
            ),
            SPOOF_STREAMING_DATA
        )

    }
)
