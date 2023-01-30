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
            context,
            "youtube",
            iconName,
            "launchericon",
            "mipmap",
            launchIcon
        )

        copyResources(
            context,
            "youtube",
            iconName,
            "splashicon",
            "drawable",
            splashIcon
        )

        monochromeIcon(
            context,
            "youtube",
            "adaptive_monochrome_ic_youtube_launcher",
            iconName
        )

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
    }

    fun customIconMusic(
        context: ResourceContext,
        iconName: String
    ) {
        val launchIcon = arrayOf(
            "adaptiveproduct_youtube_music_background_color_108",
            "adaptiveproduct_youtube_music_foreground_color_108",
            "ic_launcher_release"
        )

        copyResources(
            context,
            "music",
            iconName,
            "launchericon",
            "mipmap",
            launchIcon
        )

        monochromeIcon(
            context,
            "music",
            "ic_app_icons_themed_youtube_music",
            iconName
        )
    }

    fun customIconMusicAdditional(
        context: ResourceContext,
        iconName: String
    ) {
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
            context,
            iconName,
            record,
            "record"
        )

        copyMusicResources(
            context,
            iconName,
            actionbarLogo,
            "action_bar_logo"
        )

        copyMusicResources(
            context,
            iconName,
            actionbarLogoRelease,
            "action_bar_logo_release"
        )
    }

    private fun copyResources(
        context: ResourceContext,
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
                    this.javaClass.classLoader.getResourceAsStream("$appName/branding/$iconName/$iconPath/$size/$name.png")!!,
                    context["res"].resolve("$directory-$size").resolve("$name.png").toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
        }
    }

    private fun monochromeIcon(
        context: ResourceContext,
        appName: String,
        monochromeIconName: String,
        iconName: String
    ){
        try {
            val relativePath = "drawable/$monochromeIconName.xml"
            Files.copy(
                this.javaClass.classLoader.getResourceAsStream("$appName/branding/$iconName/monochromeicon/$relativePath")!!,
                context["res"].resolve(relativePath).toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        } catch (_: Exception) {}
    }

    private fun copyMusicResources(
        context: ResourceContext,
        iconName: String,
        iconArray: Array<String>,
        resourceNames: String
    ){
        iconArray.forEach { path ->
            val relativePath = "drawable-$path/$resourceNames.png"

            Files.copy(
                this.javaClass.classLoader.getResourceAsStream("music/branding/$iconName/resource/$relativePath")!!,
                context["res"].resolve(relativePath).toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }
}