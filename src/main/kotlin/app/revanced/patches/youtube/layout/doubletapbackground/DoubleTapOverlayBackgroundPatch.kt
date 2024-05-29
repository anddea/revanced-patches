package app.revanced.patches.youtube.layout.doubletapbackground

import app.revanced.patcher.data.ResourceContext
import app.revanced.patches.shared.overlaybackground.OverlayBackgroundUtils.removeOverlayBackground
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseResourcePatch

@Suppress("unused")
object DoubleTapOverlayBackgroundPatch : BaseResourcePatch(
    name = "Hide double tap overlay filter",
    description = "Removes, at compile time, the dark overlay that appears when double-tapping to seek.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
) {
    override fun execute(context: ResourceContext) {

        context.removeOverlayBackground(
            arrayOf("quick_seek_overlay.xml"),
            arrayOf("tap_bloom_view", "dark_background")
        )

        SettingsPatch.updatePatchStatus(this)
    }
}
