/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/2618
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.patches.music.misc.potoken

import app.morphe.patches.music.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE_MUSIC
import app.morphe.patches.music.utils.extension.sharedExtensionPatch
import app.morphe.patches.music.utils.fix.streamingdata.spoofStreamingDataPatch
import app.morphe.patches.music.utils.patch.PatchList.POTOKEN_PROVIDER
import app.morphe.patches.music.utils.settings.CategoryType
import app.morphe.patches.music.utils.settings.ResourceUtils
import app.morphe.patches.music.utils.settings.addCustomPreference
import app.morphe.patches.music.utils.settings.settingsPatch
import app.morphe.patches.shared.misc.potoken.poTokenProviderPatch

@Suppress("unused")
val poTokenProviderPatch = poTokenProviderPatch(
    name = POTOKEN_PROVIDER.title,
    description = POTOKEN_PROVIDER.summary,
    block = {
        compatibleWith(COMPATIBILITY_YOUTUBE_MUSIC)

        dependsOn(
            sharedExtensionPatch,
            settingsPatch,
            spoofStreamingDataPatch,
        )
    },
    executeBlock = {
        addCustomPreference(
            category = CategoryType.MISC,
            key = "morphe_potoken_provider",
            tag = "app.morphe.extension.shared.settings.preference.PoTokenProviderPreference",
            insertBeforeKey = "morphe_spoof_video_streams",
        )
        ResourceUtils.updatePatchStatus(POTOKEN_PROVIDER)
    }
)
