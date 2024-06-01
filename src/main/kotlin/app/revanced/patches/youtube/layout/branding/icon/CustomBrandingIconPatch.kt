package app.revanced.patches.youtube.layout.branding.icon

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.stringPatchOption
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.settings.ResourceUtils.updatePatchStatusIcon
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.ResourceGroup
import app.revanced.util.copyResources
import app.revanced.util.patch.BaseResourcePatch
import org.w3c.dom.Element
import java.io.File
import java.nio.file.Files

@Suppress("DEPRECATION", "unused")
object CustomBrandingIconPatch : BaseResourcePatch(
    name = "Custom branding icon YouTube",
    description = "Changes the YouTube app icon to the icon specified in options.json.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    use = false,
) {
    private const val DEFAULT_ICON_KEY = "Revancify Blue"

    private val availableIcon = mapOf(
        "AFN Blue" to "afn_blue",
        "AFN Red" to "afn_red",
        "MMT" to "mmt",
        DEFAULT_ICON_KEY to "revancify_blue",
        "Revancify Red" to "revancify_red",
        "Revancify Yellow" to "revancify_yellow",
        "Vanced Black" to "vanced_black",
        "Vanced Light" to "vanced_light"
    )

    private val drawableIconResourceFileNames = arrayOf(
        "product_logo_youtube_color_24",
        "product_logo_youtube_color_36",
        "product_logo_youtube_color_144",
        "product_logo_youtube_color_192",
        "yt_premium_wordmark_header_dark",
        "yt_premium_wordmark_header_light",
        "yt_wordmark_header_dark",
        "yt_wordmark_header_light"
    ).map { "$it.png" }.toTypedArray()

    private val drawableIconResourceFileNamesRevancify = arrayOf(
        "product_logo_youtube_color_24",
        "product_logo_youtube_color_36",
        "product_logo_youtube_color_144",
        "product_logo_youtube_color_192"
    ).map { "$it.png" }.toTypedArray()

    private val drawableDirectories = arrayOf(
        "xxxhdpi",
        "xxhdpi",
        "xhdpi",
        "hdpi",
        "mdpi"
    ).map { "drawable-$it" }

    private val mipmapIconResourceFileNames = arrayOf(
        "adaptiveproduct_youtube_background_color_108",
        "adaptiveproduct_youtube_foreground_color_108",
        "ic_launcher",
        "ic_launcher_round"
    ).map { "$it.png" }.toTypedArray()

    private val mipmapDirectories = arrayOf(
        "xxxhdpi",
        "xxhdpi",
        "xhdpi",
        "hdpi",
        "mdpi"
    ).map { "mipmap-$it" }

    var AppIcon by stringPatchOption(
        key = "AppIcon",
        default = DEFAULT_ICON_KEY,
        values = availableIcon,
        title = "App icon",
        description = """
            The path to a folder containing the following folders:

            ${mipmapDirectories.joinToString("\n") { "- $it" }}

            Each of these folders has to have the following files:

            ${mipmapIconResourceFileNames.joinToString("\n") { "- $it" }}
            """
            .split("\n")
            .joinToString("\n") { it.trimIndent() } // Remove the leading whitespace from each line.
            .trimIndent(), // Remove the leading newline.
    )

    override fun execute(context: ResourceContext) {
        AppIcon?.let { appIcon ->
            val appIconValue = appIcon.lowercase().replace(" ", "_")
            if (!availableIcon.containsValue(appIconValue)) {
                mipmapDirectories.map { directory ->
                    ResourceGroup(
                        directory, *mipmapIconResourceFileNames
                    )
                }.let { resourceGroups ->
                    try {
                        val path = File(appIcon)
                        val resourceDirectory = context["res"]

                        resourceGroups.forEach { group ->
                            val fromDirectory = path.resolve(group.resourceDirectoryName)
                            val toDirectory = resourceDirectory.resolve(group.resourceDirectoryName)

                            group.resources.forEach { iconFileName ->
                                Files.write(
                                    toDirectory.resolve(iconFileName).toPath(),
                                    fromDirectory.resolve(iconFileName).readBytes()
                                )
                            }
                        }
                        context.updatePatchStatusIcon("custom")
                    } catch (_: Exception) {
                        throw PatchException("Invalid app icon path: $appIcon")
                    }
                }
            } else {
                val resourcePath = "youtube/branding/$appIconValue"

                // change launcher icon.
                mipmapDirectories.map { directory ->
                    ResourceGroup(
                        directory, *mipmapIconResourceFileNames
                    )
                }.let { resourceGroups ->
                    resourceGroups.forEach {
                        context.copyResources("$resourcePath/launcher", it)
                    }
                }

                // change splash icon.
                val drawableAnimResourceFileNames = Array(5) { index -> "\$avd_anim__$index.xml" }

                val splashResourceGroups: Array<Array<ResourceGroup>> = when (appIconValue) {
                    "mmt" -> arrayOf(arrayOf(ResourceGroup("drawable", "avd_anim.xml", *drawableAnimResourceFileNames))
                        .plus(drawableDirectories.map { ResourceGroup(it, *drawableIconResourceFileNames) })
                    )

                    "revancify_blue", "revancify_red", "revancify_yellow" -> arrayOf(drawableDirectories.map {
                        ResourceGroup(
                            it,
                            *drawableIconResourceFileNamesRevancify
                        )
                    }.toTypedArray())

                    else -> arrayOf(drawableDirectories.map { ResourceGroup(it, *drawableIconResourceFileNames) }
                        .toTypedArray())
                }

                splashResourceGroups.forEach { group ->
                    group.forEach { context.copyResources("$resourcePath/splash", it) }
                }

                // monochrome
                val monochromeIcon = ResourceGroup("drawable", "adaptive_monochrome_ic_youtube_launcher.xml")
                if (appIconValue in listOf("mmt", "revancify_blue", "revancify_red", "revancify_yellow")) {
                    context.copyResources("$resourcePath/monochrome", monochromeIcon)
                }

                // disable splash animation
                if (appIconValue != "mmt") {
                    context.xmlEditor["res/values-v31/styles.xml"].use { editor ->
                        val nodeList = editor.file.getElementsByTagName("item")
                        val tags = (0 until nodeList.length).map { nodeList.item(it) as Element }
                        tags.filter { it.getAttribute("name").contains("android:windowSplashScreenAnimatedIcon") }
                            .forEach { it.parentNode.removeChild(it) }
                    }
                }

                context.updatePatchStatusIcon(appIconValue)
            }
        } ?: throw PatchException("Invalid app icon path.")
    }
}
