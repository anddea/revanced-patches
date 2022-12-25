package app.revanced.shared.util.resources

import app.revanced.patcher.data.ResourceContext
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import org.w3c.dom.Element

internal object IconHelper {

    fun customIcon(
        context: ResourceContext,
        iconName: String
    ) {
        val classLoader = this.javaClass.classLoader
        val resDirectory = context["res"]

        val Appnames = arrayOf(
            "adaptiveproduct_youtube_background_color_108",
            "adaptiveproduct_youtube_foreground_color_108",
            "ic_launcher",
            "ic_launcher_round"
        )

        val Splashnames = arrayOf(
            "product_logo_youtube_color_24",
            "product_logo_youtube_color_36",
            "product_logo_youtube_color_144",
            "product_logo_youtube_color_192"
        )

        mapOf(
            "xxxhdpi" to 192,
            "xxhdpi" to 144,
            "xhdpi" to 96,
            "hdpi" to 72,
            "mdpi" to 48
        ).forEach { (iconDirectory, size) ->
            Appnames.forEach iconLoop@{ name ->
                Files.copy(
                    classLoader.getResourceAsStream("youtube/branding/$iconName/launchericon/$size/$name.png")!!,
                    resDirectory.resolve("mipmap-$iconDirectory").resolve("$name.png").toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
            Splashnames.forEach iconLoop@{ name ->
                Files.copy(
                    classLoader.getResourceAsStream("youtube/branding/$iconName/splashicon/$size/$name.png")!!,
                    resDirectory.resolve("drawable-$iconDirectory").resolve("$name.png").toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
        }

        context.xmlEditor["res/values-v31/styles.xml"].use { editor ->
            with(editor.file) {
                val resourcesNode = getElementsByTagName("resources").item(0) as Element

                for (i in 0 until resourcesNode.childNodes.length) {
                    val node = resourcesNode.childNodes.item(i) as? Element ?: continue

                    if (node.getAttribute("name") == "Base.Theme.YouTube.Launcher") {
                        node.removeChild(node.childNodes.item(0))
                    }
                }
            }
        }

        try {
            arrayOf("drawable" to arrayOf("adaptive_monochrome_ic_youtube_launcher")).forEach { (path, resourceNames) ->
                resourceNames.forEach { name ->
                    val relativePath = "$path/$name.xml"

                     Files.copy(
                         classLoader.getResourceAsStream("youtube/branding/$iconName/monochromeicon/$relativePath")!!,
                         context["res"].resolve(relativePath).toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                    )
                }
            }
        } catch (_: Exception) {}
    }
}