/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/2695
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.patches.youtube.misc.refreshrate

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patches.shared.misc.refreshrate.baseAppRefreshRatePatch
import app.morphe.patches.youtube.shared.YouTubeActivityOnCreateFingerprint
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.utils.extension.sharedExtensionPatch
import app.morphe.patches.youtube.utils.patch.PatchList.APP_REFRESH_RATE
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch

private const val EXTENSION_CLASS = "Lapp/morphe/extension/youtube/patches/AppRefreshRatePatch;"

@Suppress("unused")
val appRefreshRatePatch = baseAppRefreshRatePatch(
    APP_REFRESH_RATE.title,
    APP_REFRESH_RATE.summary,
    block = {
        dependsOn(
            sharedExtensionPatch,
            settingsPatch
        )

        compatibleWith(COMPATIBILITY_YOUTUBE)
    },
    executeBlock = {
        addPreference(
            arrayOf(
                "SETTINGS: APP_REFRESH_RATE"
            ),
            APP_REFRESH_RATE
        )

        YouTubeActivityOnCreateFingerprint.method.addInstruction(
            0,
            "invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS->" +
                    "initialize(Landroid/app/Activity;)V",
        )
    }
)
