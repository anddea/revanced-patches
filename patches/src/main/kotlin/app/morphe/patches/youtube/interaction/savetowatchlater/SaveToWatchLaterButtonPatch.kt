/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.interaction.savetowatchlater

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.youtube.general.downloads.downloadActionsPatch
import app.morphe.patches.youtube.player.comments.commentsComponentPatch
import app.morphe.patches.youtube.utils.auth.authHookPatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.utils.patch.PatchList.SAVE_TO_WATCH_LATER
import app.morphe.patches.youtube.utils.playercontrols.addTopControl
import app.morphe.patches.youtube.utils.playercontrols.injectControl
import app.morphe.patches.youtube.utils.playercontrols.playerControlsPatch
import app.morphe.patches.youtube.utils.playlist.playlistPatch
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.patches.youtube.video.information.videoInformationPatch
import app.morphe.util.ResourceGroup
import app.morphe.util.copyResources

private val saveToWatchLaterButtonResourcePatch = resourcePatch {
    dependsOn(
        settingsPatch,
        playerControlsPatch,
    )

    execute {
        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: PLAYER",
                "PREFERENCE_SCREENS: PLAYER_BUTTONS",
                "SETTINGS: SAVE_TO_WATCH_LATER",
            ),
            SAVE_TO_WATCH_LATER,
        )

        copyResources(
            "youtube/savetowatchlaterbutton/default",
            ResourceGroup(
                resourceDirectoryName = "drawable",
                "morphe_save_to_watch_later_button.xml",
                "morphe_save_to_watch_later_button_bold.xml",
            ),
        )
    }
}

private const val EXTENSION_BUTTON_DESCRIPTOR =
    "Lapp/morphe/extension/youtube/videoplayer/SaveToWatchLaterButton;"

@Suppress("unused")
val saveToWatchLaterButtonPatch = bytecodePatch(
    SAVE_TO_WATCH_LATER.title,
    SAVE_TO_WATCH_LATER.summary,
) {
    dependsOn(
        saveToWatchLaterButtonResourcePatch,
        playerControlsPatch,
        playlistPatch,
        videoInformationPatch,
        authHookPatch,
        downloadActionsPatch,
        commentsComponentPatch,
        bytecodePatch {
            finalize {
                addTopControl(
                    "youtube/savetowatchlaterbutton/shared",
                    "@+id/morphe_save_to_watch_later_button",
                    "@+id/morphe_save_to_watch_later_button",
                )
            }
        },
    )

    compatibleWith(COMPATIBILITY_YOUTUBE)

    execute {
        injectControl(EXTENSION_BUTTON_DESCRIPTOR)
    }
}
