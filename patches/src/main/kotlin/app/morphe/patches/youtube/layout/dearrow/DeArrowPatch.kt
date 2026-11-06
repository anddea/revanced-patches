/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/3447
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.layout.dearrow

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.litho.relayout.lithoRelayoutPatch
import app.morphe.patches.youtube.layout.originaltitles.restoreOriginalTitlesPatch
import app.morphe.patches.shared.imageurl.addImageUrlErrorCallbackHook
import app.morphe.patches.shared.imageurl.addImageUrlHook
import app.morphe.patches.shared.imageurl.addImageUrlSuccessCallbackHook
import app.morphe.patches.shared.imageurl.cronetImageUrlHookPatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.utils.extension.Constants.DEARROW_CLASS_DESCRIPTOR
import app.morphe.patches.youtube.utils.navigation.navigationBarHookPatch
import app.morphe.patches.youtube.utils.patch.PatchList.DEARROW
import app.morphe.patches.youtube.utils.playertype.playerTypeHookPatch
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch

@Suppress("unused")
val deArrowPatch = bytecodePatch(
    DEARROW.title,
    DEARROW.summary,
) {
    compatibleWith(COMPATIBILITY_YOUTUBE)

    dependsOn(
        cronetImageUrlHookPatch(true),
        navigationBarHookPatch,
        playerTypeHookPatch,
        settingsPatch,
        // Thumbnails that fail to load are loaded again by mounting the Litho views again.
        lithoRelayoutPatch,
        restoreOriginalTitlesPatch,
    )
    execute {

        addImageUrlHook(DEARROW_CLASS_DESCRIPTOR)
        addImageUrlSuccessCallbackHook(DEARROW_CLASS_DESCRIPTOR)
        addImageUrlErrorCallbackHook(DEARROW_CLASS_DESCRIPTOR)

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: DEARROW",
                "SETTINGS: DEARROW"
            ),
            DEARROW
        )

        // endregion

    }
}
