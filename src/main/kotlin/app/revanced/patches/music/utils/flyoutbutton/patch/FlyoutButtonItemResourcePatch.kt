package app.revanced.patches.music.utils.flyoutbutton.patch

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.util.resources.ResourceUtils
import app.revanced.util.resources.ResourceUtils.copyResources

class FlyoutButtonItemResourcePatch : ResourcePatch {
    override fun execute(context: ResourceContext) {

        fun copyResources(resourceGroups: List<ResourceUtils.ResourceGroup>) {
            resourceGroups.forEach { context.copyResources("music/flyout", it) }
        }

        val resourceFileNames = arrayOf(
            "yt_outline_youtube_logo_icon_black_24"
        ).map { "$it.png" }.toTypedArray()

        fun createGroup(directory: String) = ResourceUtils.ResourceGroup(
            directory, *resourceFileNames
        )

        arrayOf("xxxhdpi", "xxhdpi", "xhdpi", "hdpi", "mdpi")
            .map { "drawable-$it" }
            .map(::createGroup)
            .let(::copyResources)

    }
}