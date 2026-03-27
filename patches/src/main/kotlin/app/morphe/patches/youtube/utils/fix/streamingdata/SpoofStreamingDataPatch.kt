package app.morphe.patches.youtube.utils.fix.streamingdata

import app.morphe.patches.shared.misc.spoof.spoofVideoStreamsPatch
import app.morphe.patches.shared.spoof.useragent.baseSpoofUserAgentPatch
import app.morphe.patches.youtube.utils.compatibility.Constants.YOUTUBE_PACKAGE_NAME
import app.morphe.patches.youtube.utils.mainactivity.mainActivityFingerprint
import app.morphe.patches.youtube.utils.playservice.is_19_34_or_greater
import app.morphe.patches.youtube.utils.playservice.is_19_50_or_greater
import app.morphe.patches.youtube.utils.playservice.is_20_10_or_greater
import app.morphe.patches.youtube.utils.playservice.is_20_14_or_greater
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.patches.youtube.video.information.videoInformationPatch
import app.morphe.patches.youtube.video.videoid.videoIdPatch

val spoofStreamingDataPatch = spoofVideoStreamsPatch(
    extensionClassDescriptor = "Lapp/morphe/extension/youtube/patches/spoof/SpoofVideoStreamsPatch;",
    mainActivityOnCreateFingerprint = mainActivityFingerprint.second,
    fixMediaFetchHotConfig = {
        is_19_34_or_greater
    },
    fixMediaFetchHotConfigAlternative = {
        // In 20.14 the flag was merged with 19.50 start playback flag.
        is_20_10_or_greater && !is_20_14_or_greater
    },
    fixParsePlaybackResponseFeatureFlag = {
        is_19_50_or_greater
    },
    block = {
        dependsOn(
            settingsPatch,
            versionCheckPatch,
            baseSpoofUserAgentPatch(YOUTUBE_PACKAGE_NAME),
            videoIdPatch,
            videoInformationPatch,
        )
    },
    executeBlock = {
        addPreference(
            arrayOf(
                "SETTINGS: SPOOF_VIDEO_STREAMS"
            )
        )
    },
)
