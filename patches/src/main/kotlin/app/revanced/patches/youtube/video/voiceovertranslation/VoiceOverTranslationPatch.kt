package app.revanced.patches.youtube.video.voiceovertranslation

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.extension.Constants.EXTENSION_PATH
import app.revanced.patches.youtube.utils.patch.PatchList.VOICE_OVER_TRANSLATION
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch
import app.revanced.patches.youtube.player.overlaybuttons.overlayButtonsPatch
import app.revanced.patches.youtube.video.information.hookVideoInformation
import app.revanced.patches.youtube.video.information.onCreateHook
import app.revanced.patches.youtube.video.information.videoInformationPatch
import app.revanced.patches.youtube.video.information.videoTimeHook

private const val EXTENSION_VOT_PATH =
    "$EXTENSION_PATH/patches/voiceovertranslation"

private const val EXTENSION_VOT_CLASS_DESCRIPTOR =
    "$EXTENSION_VOT_PATH/VoiceOverTranslationPatch;"

val voiceOverTranslationBytecodePatch = bytecodePatch(
    description = "voiceOverTranslationBytecodePatch"
) {
    dependsOn(
        videoInformationPatch,
    )

    execute {
        // Hook video time updates for audio sync
        videoTimeHook(
            EXTENSION_VOT_CLASS_DESCRIPTOR,
            "setVideoTime"
        )

        // Hook player initialization
        onCreateHook(
            EXTENSION_VOT_CLASS_DESCRIPTOR,
            "initialize"
        )

        // Hook new video started event to trigger translation
        hookVideoInformation(
            "$EXTENSION_VOT_CLASS_DESCRIPTOR->newVideoStarted(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JZ)V"
        )
    }
}

@Suppress("unused")
val voiceOverTranslationPatch = resourcePatch(
    VOICE_OVER_TRANSLATION.title,
    VOICE_OVER_TRANSLATION.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        overlayButtonsPatch,
        voiceOverTranslationBytecodePatch,
        settingsPatch,
    )

    execute {
        /**
         * Add settings
         */
        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: VOICE_OVER_TRANSLATION"
            ),
            VOICE_OVER_TRANSLATION
        )
    }
}
