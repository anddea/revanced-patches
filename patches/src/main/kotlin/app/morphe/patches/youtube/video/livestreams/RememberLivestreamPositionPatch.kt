/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/2753
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.patches.youtube.video.livestreams

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.utils.extension.sharedExtensionPatch
import app.morphe.patches.youtube.utils.patch.PatchList.REMEMBER_LIVESTREAM_POSITION
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.patches.youtube.video.information.onCreateHook
import app.morphe.patches.youtube.video.information.videoInformationPatch
import app.morphe.patches.youtube.video.information.videoTimeHook

private const val EXTENSION_CLASS =
    "Lapp/morphe/extension/youtube/patches/playback/livestreams/RememberLivestreamPositionPatch;"

@Suppress("unused")
val rememberLivestreamPositionPatch = bytecodePatch(
    REMEMBER_LIVESTREAM_POSITION.title,
    REMEMBER_LIVESTREAM_POSITION.summary,
) {
    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
        videoInformationPatch,
    )

    compatibleWith(COMPATIBILITY_YOUTUBE)

    execute {
        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: VIDEO",
                "SETTINGS: REMEMBER_LIVESTREAM_POSITION"
            ),
            REMEMBER_LIVESTREAM_POSITION
        )

        // Hook called when a new video starts playing (player controller created).
        onCreateHook(EXTENSION_CLASS, "newVideoStarted")

        // Hook called approximately once per second with the current playback time.
        videoTimeHook(EXTENSION_CLASS, "videoTimeChanged")
    }
}
