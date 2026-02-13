package app.morphe.patches.music.utils.fix.streamingdata

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
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
import app.morphe.patches.music.video.playerresponse.Hook
import app.morphe.patches.music.video.playerresponse.addPlayerResponseMethodHook
import app.morphe.patches.shared.buildRequestFingerprint
import app.morphe.patches.shared.buildRequestParentFingerprint
import app.morphe.patches.shared.indexOfNewUrlRequestBuilderInstruction
import app.morphe.patches.shared.spoof.streamingdata.EXTENSION_CLASS_DESCRIPTOR
import app.morphe.patches.shared.spoof.streamingdata.spoofStreamingDataPatch
import app.morphe.util.fingerprint.methodOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

val spoofStreamingDataPatch = spoofStreamingDataPatch(
    block = {
        dependsOn(
            settingsPatch,
            versionCheckPatch,
            videoInformationPatch,
            webViewPatch,
        )
    },
    isYouTube = {
        false
    },
    outlineIcon = {
        false
    },
    fixMediaFetchHotConfigChanges = {
        is_7_16_or_greater
    },
    fixMediaFetchHotConfigAlternativeChanges = {
        // In 8.15 the flag was merged with 7.33 start playback flag.
        is_8_12_or_greater && !is_8_15_or_greater
    },
    fixParsePlaybackResponseFeatureFlag = {
        is_7_33_or_greater
    },
    executeBlock = {

        // region Get replacement streams at player requests.

        buildRequestFingerprint.methodOrThrow(buildRequestParentFingerprint).apply {
            val newRequestBuilderIndex = indexOfNewUrlRequestBuilderInstruction(this)
            val urlRegister =
                getInstruction<FiveRegisterInstruction>(newRequestBuilderIndex).registerD

            addInstructions(
                newRequestBuilderIndex,
                "invoke-static { v$urlRegister, p1 }, $EXTENSION_CLASS_DESCRIPTOR->fetchStreams(Ljava/lang/String;Ljava/util/Map;)V"
            )
        }

        // endregion

        addPlayerResponseMethodHook(
            Hook.ProtoBufferParameterBeforeVideoId(
                "$EXTENSION_CLASS_DESCRIPTOR->newPlayerResponseParameter(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"
            )
        )
        addSwitchPreference(
            CategoryType.MISC,
            "revanced_spoof_streaming_data",
            "true"
        )
        addPreferenceWithIntent(
            CategoryType.MISC,
            "revanced_spoof_streaming_data_default_client",
            "revanced_spoof_streaming_data",
        )
        addPreferenceWithIntent(
            CategoryType.MISC,
            "revanced_spoof_streaming_data_sign_in_android_no_sdk_about",
            "revanced_spoof_streaming_data"
        )
        addPreferenceWithIntent(
            CategoryType.MISC,
            "revanced_spoof_streaming_data_sign_in_android_vr_about",
            "revanced_spoof_streaming_data"
        )
    },
)
