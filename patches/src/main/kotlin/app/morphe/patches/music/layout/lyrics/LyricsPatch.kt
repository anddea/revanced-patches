/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/2269
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.patches.music.layout.lyrics

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.music.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE_MUSIC_THIRD_PARTY_LYRICS
import app.morphe.patches.music.utils.extension.sharedExtensionPatch
import app.morphe.patches.music.utils.patch.PatchList.THIRD_PARTY_LYRICS
import app.morphe.patches.music.utils.playservice.is_9_00_or_greater
import app.morphe.patches.music.utils.playservice.versionCheckPatch
import app.morphe.patches.music.utils.settings.CategoryType
import app.morphe.patches.music.utils.settings.ResourceUtils
import app.morphe.patches.music.utils.settings.addCustomPreference
import app.morphe.patches.music.utils.settings.addListPreference
import app.morphe.patches.music.utils.settings.addNonInteractivePreference
import app.morphe.patches.music.utils.settings.addSwitchPreference
import app.morphe.patches.music.utils.settings.settingsPatch
import app.morphe.patches.music.video.information.videoInformationPatch
import app.morphe.patches.shared.litho.addLithoFilter
import app.morphe.patches.shared.litho.lithoFilterPatch
import app.morphe.patches.shared.misc.media.MediaSessionSetMetadataFingerprint
import app.morphe.patches.shared.misc.media.MediaSessionSetPlaybackStateFingerprint
import app.morphe.patches.shared.misc.media.hookMediaSessionArgument
import app.morphe.util.ResourceGroup
import app.morphe.util.Utils.printWarn
import app.morphe.util.copyResources

private const val EXTENSION_CLASS = "Lapp/morphe/extension/music/patches/lyrics/LyricsPatch;"

private const val LYRICS_PANEL_FILTER =
    "Lapp/morphe/extension/music/patches/components/LyricsPanelFilter;"

@Suppress("unused")
val lyricsPatch = bytecodePatch(
    name = "Third-party lyrics",
    description = "Adds an option to show synced lyrics from LRCLIB or KuGou in the lyrics panel."
) {
    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
        lithoFilterPatch,
        videoInformationPatch,
        versionCheckPatch,
        // The copy button needs its icon whether or not the patch that owns
        // these resources is applied.
        resourcePatch {
            execute {
                copyResources(
                    "music/lyrics",
                    ResourceGroup("drawable", "morphe_yt_copy_bold.xml")
                )
            }
        }
    )

    compatibleWith(COMPATIBILITY_YOUTUBE_MUSIC_THIRD_PARTY_LYRICS)

    execute {
        if (!is_9_00_or_greater) {
            printWarn("\"${THIRD_PARTY_LYRICS.title}\" is not supported in this version. Use YouTube Music 9.00.00 or later.")
            return@execute
        }

        ResourceUtils.updatePatchStatus(THIRD_PARTY_LYRICS)

        addSwitchPreference(
            CategoryType.LYRICS,
            "morphe_music_lyrics_enabled",
            "false"
        )
        addListPreference(
            CategoryType.LYRICS,
            "morphe_music_lyrics_source",
            "morphe_music_lyrics_enabled",
            false
        )
        addSwitchPreference(
            CategoryType.LYRICS,
            "morphe_music_lyrics_tap_to_seek",
            "true",
            "morphe_music_lyrics_enabled"
        )
        addSwitchPreference(
            CategoryType.LYRICS,
            "morphe_music_lyrics_show_copy_button",
            "true",
            "morphe_music_lyrics_enabled",
            false
        )
        addSwitchPreference(
            CategoryType.LYRICS,
            "morphe_music_lyrics_show_translate_button",
            "true",
            "morphe_music_lyrics_enabled",
            false
        )
        addCustomPreference(
            CategoryType.LYRICS,
            "morphe_music_lyrics_text_size",
            "app.morphe.extension.shared.settings.preference.SeekBarPreference",
            "morphe_music_lyrics_enabled"
        )
        addCustomPreference(
            CategoryType.LYRICS,
            "morphe_music_lyrics_offset_ms",
            "app.morphe.extension.shared.settings.preference.SeekBarPreference",
            "morphe_music_lyrics_enabled"
        )
        addNonInteractivePreference(
            CategoryType.LYRICS,
            "morphe_music_lyrics_about",
            "morphe_music_lyrics_enabled"
        )

        // The panel content is built by Elements, so there is no view to hook. The timed
        // lyrics component is the earliest signal that the opened panel is the lyrics one.
        addLithoFilter(LYRICS_PANEL_FILTER)

        MediaSessionSetMetadataFingerprint.hookMediaSessionArgument(
            "$EXTENSION_CLASS->onSetMetadata(Landroid/media/MediaMetadata;)V"
        )

        MediaSessionSetPlaybackStateFingerprint.hookMediaSessionArgument(
            "$EXTENSION_CLASS->onSetPlaybackState(Landroid/media/session/PlaybackState;)V"
        )
    }
}
