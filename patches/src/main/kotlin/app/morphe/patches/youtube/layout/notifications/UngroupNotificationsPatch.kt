/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/4896
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.patches.youtube.layout.notifications

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.youtube.utils.patch.PatchList.UNGROUP_NOTIFICATIONS
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.extension.sharedExtensionPatch
import app.morphe.patches.youtube.utils.playertype.playerTypeHookPatch
import app.morphe.patches.youtube.utils.playservice.is_21_40_or_greater
import app.morphe.patches.youtube.utils.playservice.is_20_21_or_greater
import app.morphe.patches.youtube.utils.playservice.is_20_05_or_greater
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.video.information.videoInformationPatch
import app.morphe.patches.youtube.video.videoid.hookBackgroundPlayVideoId
import app.morphe.patches.youtube.video.videoid.hookVideoId
import app.morphe.patches.youtube.video.videoid.videoIdPatch

private const val EXTENSION_CLASS =
    "Lapp/morphe/extension/youtube/patches/UngroupNotificationsPatch;"

@Suppress("unused")
val ungroupNotificationsPatch = bytecodePatch(
    name = UNGROUP_NOTIFICATIONS.title,
    description = UNGROUP_NOTIFICATIONS.summary,
) {
    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
        versionCheckPatch,
        videoIdPatch,
        videoInformationPatch,
        playerTypeHookPatch,
    )

    compatibleWith(COMPATIBILITY_YOUTUBE)

    execute {
        addPreference(
            arrayOf("PREFERENCE_SCREEN: FEED", "SETTINGS: UNGROUP_NOTIFICATIONS"),
            UNGROUP_NOTIFICATIONS
        )

        val onVideoLoaded = "$EXTENSION_CLASS->onVideoLoaded(Ljava/lang/String;)V"
        hookVideoId(onVideoLoaded)
        hookBackgroundPlayVideoId(onVideoLoaded)

        val buffersConstructorFingerprint = if (is_21_40_or_greater) {
            NetworkResponseBuffersConstructorFingerprint
        } else {
            NetworkResponseBuffersConstructorLegacyFingerprint
        }

        // The body is the second parameter of every supported constructor.
        val constructors = if (is_20_21_or_greater) listOf(
            NetworkResponseBytesConstructorFingerprint to "[B",
            buffersConstructorFingerprint to "Ljava/lang/Iterable;",
        ) else listOf(
            (if (is_20_05_or_greater) NetworkResponseBytesConstructorOldFingerprint
            else NetworkResponseBytesConstructorV1928Fingerprint) to "[B"
        )

        constructors.forEach { (fingerprint, bodyType) ->
            fingerprint.method.addInstructions(
                0,
                """
                    invoke-static { p2 }, $EXTENSION_CLASS->ungroupNotifications($bodyType)$bodyType
                    move-result-object p2
                """
            )
        }
    }
}
