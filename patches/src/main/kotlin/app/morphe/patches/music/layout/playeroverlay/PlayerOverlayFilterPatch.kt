package app.morphe.patches.music.layout.playeroverlay

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.music.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE_MUSIC
import app.morphe.patches.music.utils.patch.PatchList.HIDE_PLAYER_OVERLAY_FILTER
import app.morphe.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.morphe.util.removeOverlayBackground

@Suppress("unused")
val playerOverlayFilterPatch = resourcePatch(
    HIDE_PLAYER_OVERLAY_FILTER.title,
    HIDE_PLAYER_OVERLAY_FILTER.summary,
    default = false,
) {
    compatibleWith(COMPATIBILITY_YOUTUBE_MUSIC)

    execute {
        removeOverlayBackground(
            arrayOf("music_controls_overlay.xml"),
            arrayOf("player_control_screen")
        )

        updatePatchStatus(HIDE_PLAYER_OVERLAY_FILTER)

    }
}
