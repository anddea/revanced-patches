package app.morphe.patches.music.utils.fix.streamingdata

import app.morphe.patches.music.utils.compatibility.Constants.YOUTUBE_MUSIC_PACKAGE_NAME
import app.morphe.patches.music.utils.mainactivity.mainActivityFingerprint
import app.morphe.patches.music.utils.playservice.is_7_16_or_greater
import app.morphe.patches.music.utils.playservice.is_7_33_or_greater
import app.morphe.patches.music.utils.playservice.is_8_12_or_greater
import app.morphe.patches.music.utils.playservice.is_8_15_or_greater
import app.morphe.patches.music.utils.playservice.versionCheckPatch
import app.morphe.patches.music.utils.settings.CategoryType
import app.morphe.patches.music.utils.settings.addPreferenceWithIntent
import app.morphe.patches.music.utils.settings.addSwitchPreference
import app.morphe.patches.music.utils.settings.settingsPatch
import app.morphe.patches.music.utils.webview.webViewPatch
import app.morphe.patches.music.video.information.videoInformationPatch
import app.morphe.patches.shared.misc.spoof.spoofVideoStreamsPatch
import app.morphe.patches.shared.spoof.useragent.baseSpoofUserAgentPatch

val spoofStreamingDataPatch = spoofVideoStreamsPatch(
    extensionClassDescriptor = "Lapp/morphe/extension/music/patches/spoof/SpoofVideoStreamsPatch;",
    mainActivityOnCreateFingerprint = mainActivityFingerprint.second,
    fixMediaFetchHotConfig = {
        is_7_16_or_greater
    },
    fixMediaFetchHotConfigAlternative = {
        // In 8.15 the flag was merged with 7.33 start playback flag.
        is_8_12_or_greater && !is_8_15_or_greater
    },
    fixParsePlaybackResponseFeatureFlag = {
        is_7_33_or_greater
    },
    block = {
        dependsOn(
            settingsPatch,
            versionCheckPatch,
            videoInformationPatch,
            webViewPatch,
            baseSpoofUserAgentPatch(YOUTUBE_MUSIC_PACKAGE_NAME),
        )
    },
    executeBlock = {
        addSwitchPreference(
            CategoryType.MISC,
            "morphe_spoof_video_streams",
            "true"
        )
        addPreferenceWithIntent(
            CategoryType.MISC,
            "morphe_spoof_video_streams_client_type",
            "morphe_spoof_video_streams",
            false,
        )
        addPreferenceWithIntent(
            CategoryType.MISC,
            "morphe_spoof_video_streams_sign_in_android_vr_about",
            "morphe_spoof_video_streams"
        )
        addSwitchPreference(
            CategoryType.MISC,
            "morphe_spoof_video_streams_disable_player_js_update",
            "false",
            "morphe_spoof_video_streams"
        )
        addPreferenceWithIntent(
            CategoryType.MISC,
            "morphe_spoof_video_streams_player_js_hash_value",
            "morphe_spoof_video_streams_disable_player_js_update"
        )
    },
)
