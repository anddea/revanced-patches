package app.revanced.patches.music.layout.overlayfilter

import app.revanced.patcher.data.ResourceContext
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.util.patch.BaseResourcePatch

@Suppress("DEPRECATION", "unused")
object OverlayFilterPatch : BaseResourcePatch(
    name = "Hide overlay filter",
    description = "Hides the dark overlay when comment, share, save to playlist, and flyout panels are open.",
    dependencies = setOf(OverlayFilterBytecodePatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    use = false
) {
    override fun execute(context: ResourceContext) {
        val styleFile = context["res/values/styles.xml"]

        styleFile.writeText(
            styleFile.readText()
                .replace(
                    "ytOverlayBackgroundMedium\">@color/yt_black_pure_opacity60",
                    "ytOverlayBackgroundMedium\">@android:color/transparent"
                )
        )
    }
}