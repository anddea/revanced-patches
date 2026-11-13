/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/3467
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.patches.youtube.video.audio

import app.morphe.patches.shared.misc.audio.silence.skipSilencePatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.utils.extension.sharedExtensionPatch
import app.morphe.patches.youtube.utils.patch.PatchList.SKIP_SILENCE
import app.morphe.patches.youtube.utils.playservice.is_21_05_or_greater
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch

@Suppress("unused")
val skipSilencePatch = skipSilencePatch(
    block = {
        dependsOn(
            sharedExtensionPatch,
            settingsPatch,
            versionCheckPatch,
        )

        compatibleWith(COMPATIBILITY_YOUTUBE)
    },
    targetCompatible = { is_21_05_or_greater },
    executeBlock = {
        addPreference(arrayOf("PREFERENCE_SCREEN: VIDEO", "SETTINGS: SKIP_SILENCE"), SKIP_SILENCE)
    },
)
