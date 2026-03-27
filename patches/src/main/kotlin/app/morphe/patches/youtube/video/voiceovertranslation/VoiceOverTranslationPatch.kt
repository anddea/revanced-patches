/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - Jav1x (https://github.com/Jav1x)
 *
 * Licensed under the GNU General Public License v3.0.
 *
 * ------------------------------------------------------------------------
 * GPLv3 Section 7 – Attribution Notice
 * ------------------------------------------------------------------------
 *
 * This file contains substantial original work by the author(s) listed above.
 *
 * In accordance with Section 7 of the GNU General Public License v3.0,
 * the following additional terms apply to this file:
 *
 * 1. Attribution (Section 7(b)): This specific copyright notice and the
 *    list of original authors above must be preserved in any copy or
 *    derivative work. You may add your own copyright notice below it,
 *    but you may not remove the original one.
 *
 * 2. Origin (Section 7(c)): Modified versions must be clearly marked as
 *    such (e.g., by adding a "Modified by" line or a new copyright notice).
 *    They must not be misrepresented as the original work.
 *
 * ------------------------------------------------------------------------
 * Version Control Acknowledgement (Non-binding Request)
 * ------------------------------------------------------------------------
 *
 * While not a legal requirement of the GPLv3, the original author(s)
 * respectfully request that ports or substantial modifications retain
 * historical authorship credit in version control systems (e.g., Git),
 * listing original author(s) appropriately and modifiers as committers
 * or co-authors.
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
        votOriginalVolumeBytecodePatch,
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
