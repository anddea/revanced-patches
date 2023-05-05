package app.revanced.util.resources

import app.revanced.extensions.doRecursively
import app.revanced.patcher.data.ResourceContext
import app.revanced.util.resources.ResourceUtils.copyResources
import app.revanced.util.resources.ResourceUtils.copyXmlNode
import org.w3c.dom.Element

internal object MicroGResourceUtils {

    internal fun ResourceContext.copyFiles(path: String) {
        fun copyResources(resourceGroups: List<ResourceUtils.ResourceGroup>) {
            resourceGroups.forEach { this.copyResources(path, it) }
        }

        val iconResourceFileNames = arrayOf(
            "ic_microg_launcher"
        ).map { "$it.png" }.toTypedArray()

        fun createGroup(directory: String) = ResourceUtils.ResourceGroup(
            directory, *iconResourceFileNames
        )

        // change the app icon
        arrayOf("xxxhdpi", "xxhdpi", "xhdpi", "hdpi", "mdpi")
            .map { "mipmap-$it" }
            .map(::createGroup)
            .let(::copyResources)

        arrayOf(
            ResourceUtils.ResourceGroup(
                "drawable",
                "ic_app_icons_themed_microg.xml",
                "ic_microg_launcher_foreground.xml"
            ),
            ResourceUtils.ResourceGroup(
                "mipmap-anydpi-v26",
                "ic_microg_launcher.xml"
            )
        ).forEach { this.copyResources(path, it) }

        this.copyXmlNode(path, "values/colors.xml", "resources")
        this.setManifestIcon()
    }

    private fun ResourceContext.setManifestIcon() {
        this.xmlEditor["AndroidManifest.xml"].use {
            val attributes = arrayOf("icon", "roundIcon")

            it.file.doRecursively {
                attributes.forEach replacement@{ replacement ->
                    if (it !is Element) return@replacement

                    it.getAttributeNode("android:$replacement")?.let { attribute ->
                        if (attribute.textContent.startsWith("@mipmap/"))
                            attribute.textContent = "@mipmap/ic_microg_launcher"
                    }
                }
            }
        }

    }
}