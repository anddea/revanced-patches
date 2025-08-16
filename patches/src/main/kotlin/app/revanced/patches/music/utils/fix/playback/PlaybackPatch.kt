package app.revanced.patches.music.utils.fix.playback

import app.revanced.patcher.patch.booleanOption
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.extension.Constants.VIDEO_PATH
import app.revanced.patches.music.utils.fix.client.patchSpoofClient
import app.revanced.patches.music.utils.fix.parameter.patchSpoofPlayerParameter
import app.revanced.patches.music.utils.fix.streamingdata.patchSpoofVideoStreams
import app.revanced.patches.music.utils.patch.PatchList.FIX_PLAYBACK
import app.revanced.patches.music.utils.playservice.is_8_20_or_greater
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.revanced.patches.music.utils.settings.addPreferenceWithIntent
import app.revanced.patches.music.utils.settings.addSwitchPreference
import app.revanced.patches.music.utils.settings.settingsPatch
import app.revanced.patches.music.video.information.videoInformationPatch
import app.revanced.patches.music.video.playerresponse.playerResponseMethodHookPatch
import app.revanced.patches.shared.customspeed.customPlaybackSpeedPatch
import app.revanced.util.Utils.printWarn
import app.revanced.util.Utils.trimIndentMultiline

@Suppress("unused")
val playbackPatch = bytecodePatch(
    FIX_PLAYBACK.title,
    FIX_PLAYBACK.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        videoInformationPatch,
        playerResponseMethodHookPatch,
        customPlaybackSpeedPatch(
            "$VIDEO_PATH/CustomPlaybackSpeedPatch;",
            5.0f
        ),
    )

    val spoofClient = booleanOption(
        key = "spoofClient",
        default = true,
        title = "Spoof client",
        description = """
            Includes the 'Spoof client' patch.
            
            Side effect:
            • Action buttons may always be hidden in YouTube Music 7.17+.
            • Audio may intermittently stutter during playback.
            • Player flyout menu may not show properly.
            
            Not supported in YouTube Music 8.20+.
            """.trimIndentMultiline(),
        required = true
    )

    val spoofPlayerParameter = booleanOption(
        key = "spoofPlayerParameter",
        default = true,
        title = "Spoof player parameter",
        description = """
            Includes the 'Spoof player parameter' patch.
            
            Side effect:
            • Sometimes the subtitles are located at the top of the player instead of the bottom.
            • This may not work for some users.
            """.trimIndentMultiline(),
        required = true
    )

    val spoofVideoStreams = booleanOption(
        key = "spoofVideoStreams",
        default = true,
        title = "Spoof video streams",
        description = """
            Includes the 'Spoof video streams' patch.
            
            Side effect:
            • App may be forced to close when using a DNS or VPN.
            • Audio may intermittently stutter during playback on adaptive bitrate streaming clients.
            """.trimIndentMultiline(),
        required = true
    )

    execute {
        val spoofClientEnabled = spoofClient.value == true
        var spoofPlayerParameterEnabled = spoofPlayerParameter.value == true
        val spoofVideoStreamsEnabled = spoofVideoStreams.value == true

        if (!spoofClientEnabled && !spoofPlayerParameterEnabled && !spoofVideoStreamsEnabled) {
            printWarn("At least one patch option must be enabled. \"${spoofPlayerParameter.title}\" patch is used.")
            spoofPlayerParameterEnabled = true
        }

        if (spoofPlayerParameterEnabled) {
            patchSpoofPlayerParameter()

            addSwitchPreference(
                CategoryType.MISC,
                "revanced_spoof_player_parameter",
                "false"
            )
        }

        if (spoofVideoStreamsEnabled) {
            patchSpoofVideoStreams()

            addSwitchPreference(
                CategoryType.MISC,
                "revanced_spoof_video_streams",
                "false"
            )
            addPreferenceWithIntent(
                CategoryType.MISC,
                "revanced_spoof_video_streams_default_client",
                "revanced_spoof_video_streams",
            )
        }

        if (spoofClientEnabled && !is_8_20_or_greater) {
            patchSpoofClient()

            addSwitchPreference(
                CategoryType.MISC,
                "revanced_spoof_client",
                "false"
            )
            addPreferenceWithIntent(
                CategoryType.MISC,
                "revanced_spoof_client_type",
                "revanced_spoof_client",
            )
        }

        updatePatchStatus(FIX_PLAYBACK)
    }
}
