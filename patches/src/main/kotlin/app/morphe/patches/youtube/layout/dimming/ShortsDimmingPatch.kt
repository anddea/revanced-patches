package app.morphe.patches.youtube.layout.dimming

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.youtube.utils.patch.PatchList.HIDE_SHORTS_DIMMING
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.util.removeOverlayBackground

@Suppress("unused")
val shortsDimmingPatch = resourcePatch(
    HIDE_SHORTS_DIMMING.title,
    HIDE_SHORTS_DIMMING.summary,
    false,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    execute {

        removeOverlayBackground(
            arrayOf("reel_player_overlay_scrims.xml"),
            arrayOf("reel_player_overlay_v2_scrims_vertical")
        )
        removeOverlayBackground(
            arrayOf("reel_watch_fragment.xml"),
            arrayOf("reel_scrim_shorts_while_top")
        )

        addPreference(HIDE_SHORTS_DIMMING)

    }
}
