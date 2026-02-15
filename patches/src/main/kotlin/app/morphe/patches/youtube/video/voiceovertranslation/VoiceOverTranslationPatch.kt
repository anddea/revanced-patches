/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of https://github.com/anddea/revanced-patches/.
 *
 * The original author: https://github.com/Jav1x.
 *
 * IMPORTANT: This file is the proprietary work of https://github.com/Jav1x.
 * Any modifications, derivatives, or substantial rewrites of this file
 * must retain this copyright notice and the original author attribution
 * in the source code and version control history.
 */

package app.morphe.patches.youtube.video.voiceovertranslation

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.youtube.utils.extension.Constants.EXTENSION_PATH
import app.morphe.patches.youtube.utils.patch.PatchList.VOICE_OVER_TRANSLATION
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.patches.youtube.player.overlaybuttons.overlayButtonsPatch
import app.morphe.patches.youtube.utils.extension.Constants.PATCH_STATUS_CLASS_DESCRIPTOR
import app.morphe.patches.youtube.video.information.hookVideoInformation
import app.morphe.patches.youtube.video.information.onCreateHook
import app.morphe.patches.youtube.video.information.videoInformationPatch
import app.morphe.patches.youtube.video.information.videoTimeHook
import app.morphe.util.updatePatchStatus

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

        // Update the patch status to enabled for the extension
        updatePatchStatus(PATCH_STATUS_CLASS_DESCRIPTOR, "VoiceOverTranslation")
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
