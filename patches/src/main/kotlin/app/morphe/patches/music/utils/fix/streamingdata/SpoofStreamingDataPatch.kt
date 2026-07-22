package app.morphe.patches.music.utils.fix.streamingdata

import app.morphe.patches.music.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE_MUSIC
import app.morphe.patches.music.utils.compatibility.Constants.YOUTUBE_MUSIC_PACKAGE_NAME
import app.morphe.patches.music.utils.mainactivity.mainActivityFingerprint
import app.morphe.patches.music.utils.playservice.is_7_16_or_greater
import app.morphe.patches.music.utils.playservice.is_7_33_or_greater
import app.morphe.patches.music.utils.playservice.is_8_12_or_greater
import app.morphe.patches.music.utils.playservice.is_8_15_or_greater
import app.morphe.patches.music.utils.playservice.versionCheckPatch
import app.morphe.patches.music.utils.settings.CategoryType
import app.morphe.patches.music.utils.settings.addCustomPreference
import app.morphe.patches.music.utils.settings.addListPreference
import app.morphe.patches.music.utils.settings.addTextPreference
import app.morphe.patches.music.utils.settings.addSwitchPreference
import app.morphe.patches.music.utils.settings.settingsPatch
import app.morphe.patches.music.utils.webview.webViewPatch
import app.morphe.patches.music.video.information.videoInformationPatch
import app.morphe.patches.shared.misc.spoof.spoofVideoStreamsPatch
import app.morphe.patches.shared.spoof.useragent.baseSpoofUserAgentPatch

@Suppress("unused")
val spoofStreamingDataPatch = spoofVideoStreamsPatch(
    // Updated parameter name: extensionClassDescriptor -> extensionClass
    extensionClass = "Lapp/morphe/extension/music/patches/spoof/SpoofVideoStreamsPatch;",
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
    // New parameters added to match the updated spoofVideoStreamsPatch signature
    fixMediaSessionFeatureFlag = {
        false
    },
    fixReelItemWatchResponseFeatureFlag = {
        false
    },
    hookAccountIdentity = {
        false
    },
    useNewRequestBuilderFingerprint = {
        false
    },
    block = {
        compatibleWith(COMPATIBILITY_YOUTUBE_MUSIC)

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
        addListPreference(
            CategoryType.MISC,
            "morphe_spoof_video_streams_client_type",
            "morphe_spoof_video_streams",
            false,
        )
        addCustomPreference(
            CategoryType.MISC,
            "morphe_spoof_video_streams_sign_in_android_vr_about",
            "app.morphe.extension.music.settings.preference.SpoofVideoStreamsSignInPreference",
            "morphe_spoof_video_streams"
        )
        addSwitchPreference(
            CategoryType.MISC,
            "morphe_spoof_video_streams_disable_player_js_update",
            "false",
            "morphe_spoof_video_streams"
        )
        addTextPreference(
            CategoryType.MISC,
            "morphe_spoof_video_streams_player_js_hash_value",
            "morphe_spoof_video_streams_disable_player_js_update"
        )
    },
)
