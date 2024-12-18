package app.revanced.patches.music.layout.playeroverlay

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.patch.PatchList.HIDE_PLAYER_OVERLAY_FILTER
import app.revanced.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.revanced.util.removeOverlayBackground

@Suppress("unused")
val playerOverlayFilterPatch = resourcePatch(
    HIDE_PLAYER_OVERLAY_FILTER.title,
    HIDE_PLAYER_OVERLAY_FILTER.summary,
    use = false,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    execute {
        removeOverlayBackground(
            arrayOf("music_controls_overlay.xml"),
            arrayOf("player_control_screen")
        )

        updatePatchStatus(HIDE_PLAYER_OVERLAY_FILTER)

    }
}

