/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
 * - inotia00 (https://github.com/inotia00)
 *
 * Licensed under the GNU General Public License v3.0.
 *
 * ------------------------------------------------------------------------
 * GPLv3 Section 7 – Additional Terms & Attribution Requirements
 * ------------------------------------------------------------------------
 *
 * This file contains substantial original work by the author(s) listed above.
 *
 * In accordance with Section 7 of the GNU General Public License v3.0,
 * the following additional terms apply to this file:
 *
 * 1. Source Credit Preservation (Section 7(b)): This specific copyright notice
 *    and the list of original authors above must be preserved in any copy
 *    or derivative work. You may add your own copyright notice below it,
 *    but you may not remove the original one.
 *
 * 2. Origin & Modification Marking (Section 7(c)): Modified versions must be
 *    clearly marked as such (e.g., by adding a "Modified by" line or a new
 *    copyright notice) and must not be misrepresented as the original work.
 *
 * 3. Version Control Attribution (Section 7(b)): Any ports or substantial
 *    modifications must retain historical authorship credit in version control
 *    systems (e.g., Git), listing original author(s) appropriately and
 *    modifiers as committers or co-authors.
 *
 * 4. User Interface Attribution (Section 7(b)): Any works containing or
 *    derived from this material must maintain a visible credit or
 *    acknowledgment to the original author(s) within the application's
 *    user interface (e.g., in an "About" or "Credits" section).
 */

package app.morphe.patches.music.utils.fix.streamingdata

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
