/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/2695
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.patches.music.misc.refreshrate

import app.morphe.patches.music.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE_MUSIC
import app.morphe.patches.music.utils.extension.sharedExtensionPatch
import app.morphe.patches.music.utils.patch.PatchList.APP_REFRESH_RATE
import app.morphe.patches.music.utils.settings.CategoryType
import app.morphe.patches.music.utils.settings.ResourceUtils
import app.morphe.patches.music.utils.settings.addCustomPreference
import app.morphe.patches.music.utils.settings.settingsPatch
import app.morphe.patches.shared.misc.refreshrate.baseAppRefreshRatePatch

@Suppress("unused")
val appRefreshRatePatch = baseAppRefreshRatePatch(
    APP_REFRESH_RATE.title,
    APP_REFRESH_RATE.summary,
    block = {
        dependsOn(
            sharedExtensionPatch,
            settingsPatch
        )

        compatibleWith(COMPATIBILITY_YOUTUBE_MUSIC)
    },
    executeBlock = {
        addCustomPreference(
            category = CategoryType.MISC,
            key = "morphe_app_refresh_rate",
            tag = "app.morphe.extension.shared.settings.preference.AppRefreshRateListPreference",
            setSummary = false
        )
        ResourceUtils.updatePatchStatus(APP_REFRESH_RATE)
    }
)
