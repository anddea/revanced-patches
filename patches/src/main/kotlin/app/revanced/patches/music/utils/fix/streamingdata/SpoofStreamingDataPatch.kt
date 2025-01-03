package app.revanced.patches.music.utils.fix.streamingdata

import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.compatibility.Constants.YOUTUBE_MUSIC_PACKAGE_NAME
import app.revanced.patches.music.utils.patch.PatchList.SPOOF_STREAMING_DATA
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.revanced.patches.music.utils.settings.addPreferenceWithIntent
import app.revanced.patches.music.utils.settings.addSwitchPreference
import app.revanced.patches.music.utils.settings.settingsPatch
import app.revanced.patches.music.video.playerresponse.hookPlayerResponse
import app.revanced.patches.music.video.playerresponse.playerResponseMethodHookPatch
import app.revanced.patches.shared.extension.Constants.PATCHES_PATH
import app.revanced.patches.shared.extension.Constants.SPOOF_PATH
import app.revanced.patches.shared.spoof.streamingdata.baseSpoofStreamingDataPatch
import app.revanced.patches.shared.spoof.useragent.baseSpoofUserAgentPatch
import app.revanced.util.findMethodOrThrow

const val EXTENSION_CLASS_DESCRIPTOR =
    "$SPOOF_PATH/SpoofStreamingDataPatch;"

@Suppress("unused")
val spoofStreamingDataPatch = baseSpoofStreamingDataPatch(
    {
        compatibleWith(COMPATIBLE_PACKAGE)

        dependsOn(
            baseSpoofUserAgentPatch(YOUTUBE_MUSIC_PACKAGE_NAME),
            settingsPatch,
            playerResponseMethodHookPatch,
        )
    },
    {
        findMethodOrThrow("$PATCHES_PATH/PatchStatus;") {
            name == "SpoofStreamingDataMusic"
        }.replaceInstruction(
            0,
            "const/4 v0, 0x1"
        )

        hookPlayerResponse(
            "$EXTENSION_CLASS_DESCRIPTOR->fetchStreams(Ljava/lang/String;)V",
            true
        )

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
