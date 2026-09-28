/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/1881
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.music.interaction.downloads

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.music.actionbar.components.actionBarComponentsPatch
import app.morphe.patches.music.flyoutmenu.components.flyoutMenuComponentsPatch
import app.morphe.patches.music.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE_MUSIC
import app.morphe.patches.music.utils.extension.sharedExtensionPatch
import app.morphe.patches.music.utils.patch.PatchList.DOWNLOADS
import app.morphe.patches.music.utils.settings.CategoryType
import app.morphe.patches.music.utils.settings.ResourceUtils.addPreferenceCategory
import app.morphe.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.morphe.patches.music.utils.settings.addSwitchPreference
import app.morphe.patches.music.utils.settings.settingsPatch
import app.morphe.patches.shared.misc.media.MediaSessionSetMetadataFingerprint
import app.morphe.patches.shared.misc.media.hookMediaSessionArgument

private val OFFLINE_PLAYBACK_PERMISSIONS = listOf(
    "android.permission.WAKE_LOCK",
    "android.permission.FOREGROUND_SERVICE",
    "android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK",
    "android.permission.POST_NOTIFICATIONS",
)

private const val OFFLINE_PLAYBACK_SERVICE =
    "app.morphe.extension.music.patches.downloads.OfflinePlaybackService"

private const val EXTENSION_CLASS =
    "Lapp/morphe/extension/music/patches/downloads/LocalDownloadManager;"

private val downloadsResourcePatch = resourcePatch {
    dependsOn(settingsPatch, actionBarComponentsPatch)

    execute {
        addPreferenceCategory(CategoryType.MISC.value)
        addSwitchPreference(
            CategoryType.MISC,
            "morphe_music_in_app_downloads",
            "false",
            "revanced_external_downloader_action"
        )

        val manifest = get("AndroidManifest.xml")
        var xml = manifest.readText()

        // YouTube Music already declares most of these, so each one is added only when missing.
        // The whole attribute is matched, since FOREGROUND_SERVICE is a prefix of
        // FOREGROUND_SERVICE_MEDIA_PLAYBACK and would otherwise look like it is already there.
        for (permission in OFFLINE_PLAYBACK_PERMISSIONS) {
            val declaration = "android:name=\"$permission\""
            if (!xml.contains(declaration)) {
                xml = xml.replace(
                    "<application",
                    "<uses-permission $declaration />\n    <application"
                )
            }
        }

        if (!xml.contains(OFFLINE_PLAYBACK_SERVICE)) {
            xml = xml.replace(
                "</application>",
                "<service android:name=\"$OFFLINE_PLAYBACK_SERVICE\" " +
                    "android:exported=\"false\" android:foregroundServiceType=\"mediaPlayback\" />\n</application>"
            )
        }

        manifest.writeText(xml)
    }
}

@Suppress("unused")
val downloadsPatch = bytecodePatch(
    DOWNLOADS.title,
    DOWNLOADS.summary,
) {
    dependsOn(
        downloadsResourcePatch,
        sharedExtensionPatch,
        settingsPatch,
        actionBarComponentsPatch,
        flyoutMenuComponentsPatch,
    )

    compatibleWith(COMPATIBILITY_YOUTUBE_MUSIC)

    execute {
        MediaSessionSetMetadataFingerprint.hookMediaSessionArgument(
            "$EXTENSION_CLASS->onSetMetadata(Landroid/media/MediaMetadata;)V"
        )

        updatePatchStatus(DOWNLOADS)
    }
}
