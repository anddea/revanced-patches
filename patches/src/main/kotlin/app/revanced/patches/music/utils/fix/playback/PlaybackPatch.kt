package app.revanced.patches.music.utils.fix.playback

import app.revanced.patcher.patch.booleanOption
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.music.misc.backgroundplayback.backgroundPlaybackPatch
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.extension.Constants.PATCH_STATUS_CLASS_DESCRIPTOR
import app.revanced.patches.music.utils.extension.Constants.SPOOF_PATH
import app.revanced.patches.music.utils.extension.Constants.VIDEO_PATH
import app.revanced.patches.music.utils.fix.client.patchSpoofClient
import app.revanced.patches.music.utils.fix.streamingdata.patchSpoofVideoStreams
import app.revanced.patches.music.utils.patch.PatchList.FIX_PLAYBACK
import app.revanced.patches.music.utils.playservice.versionCheckPatch
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.revanced.patches.music.utils.settings.addPreferenceWithIntent
import app.revanced.patches.music.utils.settings.addSwitchPreference
import app.revanced.patches.music.utils.settings.settingsPatch
import app.revanced.patches.shared.customspeed.customPlaybackSpeedPatch
import app.revanced.patches.shared.spoof.blockrequest.baseBlockRequestPatch
import app.revanced.util.Utils.printWarn
import app.revanced.util.Utils.trimIndentMultiline
import app.revanced.util.findMethodOrThrow
import app.revanced.util.returnEarly

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$SPOOF_PATH/BlockRequestPatch;"

@Suppress("unused")
val playbackPatch = bytecodePatch(
    FIX_PLAYBACK.title,
    FIX_PLAYBACK.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        // required to fix background playback issue of live stream on iOS client.
        backgroundPlaybackPatch,
        baseBlockRequestPatch(EXTENSION_CLASS_DESCRIPTOR),
        customPlaybackSpeedPatch(
            "$VIDEO_PATH/CustomPlaybackSpeedPatch;",
            5.0f
        ),
        versionCheckPatch,
    )

    val spoofClient = booleanOption(
        key = "spoofClient",
        default = true,
        title = "Spoof client",
        description = """
            Includes the 'Spoof client' patch.
            
            Side effect:
            • Action buttons or player flyout menus may not show properly.
            • These side effects may be resolved by clearing the app data and logging in again.
            
            Side effect (Block request):
            • App may be forced to close when using a VPN or DNS.
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
        var spoofClientEnabled = spoofClient.value == true
        val spoofVideoStreamsEnabled = spoofVideoStreams.value == true

        if (!spoofClientEnabled && !spoofVideoStreamsEnabled) {
            printWarn("At least one patch option must be enabled. \"${spoofClient.title}\" patch is used.")
            spoofClientEnabled = true
        }

        if (spoofClientEnabled) {
            patchSpoofClient()

            findMethodOrThrow(PATCH_STATUS_CLASS_DESCRIPTOR) {
                name == "SpoofClient"
            }.returnEarly(true)

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

        if (spoofVideoStreamsEnabled) {
            patchSpoofVideoStreams()

            findMethodOrThrow(PATCH_STATUS_CLASS_DESCRIPTOR) {
                name == "SpoofVideoStreams"
            }.returnEarly(true)

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

        updatePatchStatus(FIX_PLAYBACK)
    }
}
