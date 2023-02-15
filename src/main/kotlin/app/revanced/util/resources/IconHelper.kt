package app.revanced.util.resources

import app.revanced.patcher.data.ResourceContext
import org.w3c.dom.Element
import java.nio.file.Files
import java.nio.file.StandardCopyOption

internal object IconHelper {

    internal fun ResourceContext.customIcon(iconName: String) {
        val launchIcon = arrayOf(
            "adaptiveproduct_youtube_background_color_108",
            "adaptiveproduct_youtube_foreground_color_108",
            "ic_launcher",
            "ic_launcher_round"
        )

        val splashIcon = arrayOf(
            "product_logo_youtube_color_24",
            "product_logo_youtube_color_36",
            "product_logo_youtube_color_144",
            "product_logo_youtube_color_192"
        )

        copyResources(
            "youtube",
            iconName,
            "launchericon",
            "mipmap",
            launchIcon
        )

        copyResources(
            "youtube",
            iconName,
            "splashicon",
            "drawable",
            splashIcon
        )

        monochromeIcon(
            "youtube",
            "adaptive_monochrome_ic_youtube_launcher",
            iconName
        )

        this.xmlEditor["res/values-v31/styles.xml"].use { editor ->
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
    }

    internal fun ResourceContext.customIconMusic(iconName: String) {
        val launchIcon = arrayOf(
            "adaptiveproduct_youtube_music_background_color_108",
            "adaptiveproduct_youtube_music_foreground_color_108",
            "ic_launcher_release"
        )

        copyResources(
            "music",
            iconName,
            "launchericon",
            "mipmap",
            launchIcon
        )

        monochromeIcon(
            "music",
            "ic_app_icons_themed_youtube_music",
            iconName
        )
    }

    internal fun ResourceContext.customIconMusicAdditional(iconName: String) {
        val record = arrayOf(
            "hdpi",
            "large-hdpi",
            "large-mdpi",
            "large-xhdpi",
            "mdpi",
            "xhdpi",
            "xlarge-hdpi",
            "xlarge-mdpi",
            "xxhdpi"
        )

        val actionbarLogo = arrayOf(
            "hdpi",
            "mdpi",
            "xhdpi",
            "xxhdpi",
            "xxxhdpi"
        )

        val actionbarLogoRelease = arrayOf(
            "hdpi"
        )

        copyMusicResources(
            iconName,
            record,
            "record"
        )

        copyMusicResources(
            iconName,
            actionbarLogo,
            "action_bar_logo"
        )

        copyMusicResources(
            iconName,
            actionbarLogoRelease,
            "action_bar_logo_release"
        )
    }

    private fun ResourceContext.copyResources(
        appName: String,
        iconName: String,
        iconPath: String,
        directory: String,
        iconArray: Array<String>
    ){
        arrayOf(
            "xxxhdpi",
            "xxhdpi",
            "xhdpi",
            "hdpi",
            "mdpi"
        ).forEach { size ->
            iconArray.forEach iconLoop@{ name ->
                Files.copy(
                    ResourceUtils.javaClass.classLoader.getResourceAsStream("$appName/branding/$iconName/$iconPath/$size/$name.png")!!,
                    this["res"].resolve("$directory-$size").resolve("$name.png").toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
        }
    }

    private fun ResourceContext.monochromeIcon(
        appName: String,
        monochromeIconName: String,
        iconName: String
    ){
        try {
            val relativePath = "drawable/$monochromeIconName.xml"
            Files.copy(
                ResourceUtils.javaClass.classLoader.getResourceAsStream("$appName/branding/$iconName/monochromeicon/$relativePath")!!,
                this["res"].resolve(relativePath).toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        } catch (_: Exception) {}
    }

    private fun ResourceContext.copyMusicResources(
        iconName: String,
        iconArray: Array<String>,
        resourceNames: String
    ){
        iconArray.forEach { path ->
            val relativePath = "drawable-$path/$resourceNames.png"

            Files.copy(
                ResourceUtils.javaClass.classLoader.getResourceAsStream("music/branding/$iconName/resource/$relativePath")!!,
                this["res"].resolve(relativePath).toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }
}