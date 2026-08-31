/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/2618
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.patches.youtube.misc.potoken

import app.morphe.patches.shared.misc.potoken.poTokenProviderPatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.utils.extension.sharedExtensionPatch
import app.morphe.patches.youtube.utils.fix.streamingdata.spoofStreamingDataPatch
import app.morphe.patches.youtube.utils.patch.PatchList.POTOKEN_PROVIDER
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch

@Suppress("unused")
val poTokenProviderPatch = poTokenProviderPatch(
    name = POTOKEN_PROVIDER.title,
    description = POTOKEN_PROVIDER.summary,
    block = {
        compatibleWith(COMPATIBILITY_YOUTUBE)

        dependsOn(
            sharedExtensionPatch,
            settingsPatch,
            spoofStreamingDataPatch,
        )
    },
    executeBlock = {
        addPreference(
            arrayOf(
                "SETTINGS: POTOKEN_PROVIDER"
            ),
            POTOKEN_PROVIDER
        )
    }
)
