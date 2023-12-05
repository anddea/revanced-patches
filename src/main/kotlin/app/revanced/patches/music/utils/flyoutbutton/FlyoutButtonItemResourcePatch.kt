package app.revanced.patches.music.utils.flyoutbutton

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.util.ResourceGroup
import app.revanced.util.copyResources

object FlyoutButtonItemResourcePatch : ResourcePatch() {
    override fun execute(context: ResourceContext) {

        fun copyResources(resourceGroups: List<ResourceGroup>) {
            resourceGroups.forEach { context.copyResources("music/flyout", it) }
        }

        val resourceFileNames = arrayOf(
            "yt_outline_youtube_logo_icon_black_24"
        ).map { "$it.png" }.toTypedArray()

        fun createGroup(directory: String) = ResourceGroup(
            directory, *resourceFileNames
        )

        arrayOf("xxxhdpi", "xxhdpi", "xhdpi", "hdpi", "mdpi")
            .map { "drawable-$it" }
            .map(::createGroup)
            .let(::copyResources)

    }
}