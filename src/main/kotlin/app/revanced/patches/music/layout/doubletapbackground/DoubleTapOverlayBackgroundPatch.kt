package app.revanced.patches.music.layout.doubletapbackground

import app.revanced.patcher.data.ResourceContext
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.shared.overlaybackground.OverlayBackgroundUtils.removeOverlayBackground
import app.revanced.util.patch.BaseResourcePatch

@Suppress("unused")
object DoubleTapOverlayBackgroundPatch : BaseResourcePatch(
    name = "Hide double tap overlay filter",
    description = "Hides the dark overlay when double-tapping to seek.",
    compatiblePackages = COMPATIBLE_PACKAGE,
    use = false
) {
    override fun execute(context: ResourceContext) {
        context.removeOverlayBackground(
            arrayOf("quick_seek_overlay.xml"),
            arrayOf("tap_bloom_view", "dark_background")
        )
    }
}