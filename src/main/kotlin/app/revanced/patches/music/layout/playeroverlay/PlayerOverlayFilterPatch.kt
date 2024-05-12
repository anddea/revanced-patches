package app.revanced.patches.music.layout.playeroverlay

import app.revanced.patcher.data.ResourceContext
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.shared.overlaybackground.OverlayBackgroundUtils.removeOverlayBackground
import app.revanced.util.patch.BaseResourcePatch

@Suppress("unused")
object PlayerOverlayFilterPatch : BaseResourcePatch(
    name = "Hide player overlay filter",
    description = "Hides the dark overlay when single-tapping player.",
    compatiblePackages = COMPATIBLE_PACKAGE,
    use = false
) {
    override fun execute(context: ResourceContext) {
        context.removeOverlayBackground(
            arrayOf("music_controls_overlay.xml"),
            arrayOf("player_control_screen")
        )
    }
}