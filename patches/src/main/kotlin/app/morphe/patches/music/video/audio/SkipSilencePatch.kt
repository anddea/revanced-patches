/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/3467
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.patches.music.video.audio

import app.morphe.patches.music.utils.extension.sharedExtensionPatch
import app.morphe.patches.music.utils.patch.PatchList.SKIP_SILENCE
import app.morphe.patches.music.utils.settings.settingsPatch
import app.morphe.patches.music.utils.settings.CategoryType
import app.morphe.patches.music.utils.settings.addSwitchPreference
import app.morphe.patches.music.utils.settings.addListPreference
import app.morphe.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.morphe.patches.music.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE_MUSIC
import app.morphe.patches.shared.misc.audio.silence.skipSilencePatch

@Suppress("unused")
val skipSilencePatch = skipSilencePatch(
    block = {
        dependsOn(
            sharedExtensionPatch,
            settingsPatch
        )

        compatibleWith(COMPATIBILITY_YOUTUBE_MUSIC)
    },
    executeBlock = {
        addSwitchPreference(CategoryType.PLAYER, "morphe_skip_silence", "false")
        addListPreference(CategoryType.PLAYER, "morphe_skip_silence_minimum_pause", "morphe_skip_silence")
        updatePatchStatus(SKIP_SILENCE)
    }
)
