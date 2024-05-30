package app.revanced.patches.youtube.layout.dimming

import app.revanced.patcher.data.ResourceContext
import app.revanced.patches.shared.overlaybackground.OverlayBackgroundUtils.removeOverlayBackground
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseResourcePatch

@Suppress("unused")
object ShortsDimmingPatch : BaseResourcePatch(
    name = "Hide Shorts dimming",
    description = "Removes, at compile time, the dimming effect at the top and bottom of Shorts videos.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    use = false
) {
    override fun execute(context: ResourceContext) {

        context.removeOverlayBackground(
            arrayOf("reel_player_overlay_scrims.xml"),
            arrayOf("reel_player_overlay_v2_scrims_vertical")
        )
        context.removeOverlayBackground(
            arrayOf("reel_watch_fragment.xml"),
            arrayOf("reel_scrim_shorts_while_top")
        )

        SettingsPatch.updatePatchStatus(this)
    }
}
