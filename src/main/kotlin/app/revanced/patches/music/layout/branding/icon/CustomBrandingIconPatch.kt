package app.revanced.patches.music.layout.branding.icon

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.booleanPatchOption
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.stringPatchOption
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.settings.ResourceUtils.setIconType
import app.revanced.util.ResourceGroup
import app.revanced.util.Utils.trimIndentMultiline
import app.revanced.util.copyResources
import app.revanced.util.getResourceGroup
import app.revanced.util.patch.BaseResourcePatch
import app.revanced.util.underBarOrThrow
import org.w3c.dom.Element
import java.io.File
import java.nio.file.Files

@Suppress("DEPRECATION", "unused")
object CustomBrandingIconPatch : BaseResourcePatch(
    name = "Custom branding icon for YouTube Music",
    description = "Changes the YouTube Music app icon to the icon specified in options.json.",
    compatiblePackages = COMPATIBLE_PACKAGE,
    use = false,
) {
    private const val DEFAULT_ICON_KEY = "Xisr Yellow"

    private val availableIcon = mapOf(
        "AFN Blue" to "afn_blue",
        "AFN Red" to "afn_red",
        "MMT" to "mmt",
        "MMT Blue" to "mmt_blue",
        "MMT Green" to "mmt_green",
        "MMT Orange" to "mmt_orange",
        "MMT Pink" to "mmt_pink",
        "MMT Turquoise" to "mmt_turquoise",
        "MMT Yellow" to "mmt_yellow",
        "Revancify Blue" to "revancify_blue",
        "Revancify Red" to "revancify_red",
        "Vanced Black" to "vanced_black",
        "Vanced Light" to "vanced_light",
        DEFAULT_ICON_KEY to "xisr_yellow", 
        "YouTube Music" to "youtube_music"
    )

    private val sizeArray = arrayOf(
        "xxxhdpi",
        "xxhdpi",
        "xhdpi",
        "hdpi",
        "mdpi"
    )

    private val largeSizeArray = arrayOf(
        "xlarge-hdpi",
        "xlarge-mdpi",
        "large-xhdpi",
        "large-hdpi",
        "large-mdpi",
        "xxhdpi",
        "xhdpi",
        "hdpi",
        "mdpi",
    )

    private val drawableDirectories = sizeArray.map { "drawable-$it" }

    private val largeDrawableDirectories = largeSizeArray.map { "drawable-$it" }

    private val mipmapDirectories = sizeArray.map { "mipmap-$it" }

    private val headerIconResourceFileNames = arrayOf(
        "action_bar_logo",
        "logo_music",
        "ytm_logo"
    ).map { "$it.png" }.toTypedArray()

    private val launcherIconResourceFileNames = arrayOf(
        "adaptiveproduct_youtube_music_background_color_108",
        "adaptiveproduct_youtube_music_foreground_color_108",
        "ic_launcher_release"
    ).map { "$it.png" }.toTypedArray()

    private val splashIconResourceFileNames = arrayOf(
        // This file only exists in [drawable-hdpi]
        // Since {@code ResourceUtils#copyResources} checks for null values before copying,
        // Just adds it to the array.
        "action_bar_logo_release",
        "record"
    ).map { "$it.png" }.toTypedArray()

    private val headerIconResourceGroups =
        drawableDirectories.getResourceGroup(headerIconResourceFileNames)

    private val launcherIconResourceGroups =
        mipmapDirectories.getResourceGroup(launcherIconResourceFileNames)

    private val splashIconResourceGroups =
        largeDrawableDirectories.getResourceGroup(splashIconResourceFileNames)

    val AppIcon = stringPatchOption(
        key = "AppIcon",
        default = DEFAULT_ICON_KEY,
        values = availableIcon,
        title = "App icon",
        description = """
            The icon to apply to the app.
            
            If a path to a folder is provided, the folder must contain the following folders:

            ${mipmapDirectories.joinToString("\n") { "- $it" }}

            Each of these folders must contain the following files:

            ${launcherIconResourceFileNames.joinToString("\n") { "- $it" }}
            """.trimIndentMultiline(),
        required = true
    )

    private val ChangeHeader by booleanPatchOption(
        key = "ChangeHeader",
        default = true,
        title = "Change header",
        description = "Apply the custom branding icon to the header.",
        required = true
    )

    private val ChangeSplashIcon by booleanPatchOption(
        key = "ChangeSplashIcon",
        default = true,
        title = "Change splash icons",
        description = "Apply the custom branding icon to the splash screen.",
        required = true
    )

    private val RestoreOldSplashIcon by booleanPatchOption(
        key = "RestoreOldSplashIcon",
        default = false,
        title = "Restore old splash icon",
        description = """
            Restore the old style splash icon.
            
            If you enable both the old style splash icon and the Cairo splash animation,
            
            Old style splash icon will appear first and then the Cairo splash animation will start.
            """.trimIndentMultiline(),
        required = true
    )

    override fun execute(context: ResourceContext) {

        // Check patch options first.
        val appIcon = AppIcon
            .underBarOrThrow()

        val appIconResourcePath = "music/branding/$appIcon"
        val youtubeMusicIconResourcePath = "music/branding/youtube_music"

        // Check if a custom path is used in the patch options.
        if (!availableIcon.containsValue(appIcon)) {
            launcherIconResourceGroups.let { resourceGroups ->
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
                } catch (_: Exception) {
                    // Exception is thrown if an invalid path is used in the patch option.
                    throw PatchException("Invalid app icon path: $appIcon")
                }
            }
        } else {

            // Change launcher icon.
            launcherIconResourceGroups.let { resourceGroups ->
                resourceGroups.forEach {
                    context.copyResources("$appIconResourcePath/launcher", it)
                }
            }

            // Change monochrome icon.
            arrayOf(
                ResourceGroup(
                    "drawable",
                    "ic_app_icons_themed_youtube_music.xml"
                )
            ).forEach { resourceGroup ->
                context.copyResources("$appIconResourcePath/monochrome", resourceGroup)
            }

            // Change header.
            if (ChangeHeader == true) {
                headerIconResourceGroups.let { resourceGroups ->
                    resourceGroups.forEach {
                        context.copyResources("$appIconResourcePath/header", it)
                    }
                }
            }

            // Change splash icon.
            if (RestoreOldSplashIcon == true) {
                var oldSplashIconNotExists: Boolean

                context.xmlEditor["res/drawable/splash_screen.xml"].use { editor ->
                    editor.file.apply {
                        val node = getElementsByTagName("layer-list").item(0)
                        oldSplashIconNotExists = (node as Element)
                            .getElementsByTagName("item")
                            .length == 1

                        if (oldSplashIconNotExists) {
                            createElement("item").also { itemNode ->
                                itemNode.appendChild(
                                    createElement("bitmap").also { bitmapNode ->
                                        bitmapNode.setAttribute("android:gravity", "center")
                                        bitmapNode.setAttribute("android:src", "@drawable/record")
                                    }
                                )
                                node.appendChild(itemNode)
                            }
                        }
                    }
                }
                if (oldSplashIconNotExists) {
                    splashIconResourceGroups.let { resourceGroups ->
                        resourceGroups.forEach {
                            context.copyResources(
                                "$youtubeMusicIconResourcePath/splash",
                                it,
                                createDirectoryIfNotExist = true
                            )
                        }
                    }
                }
            }

            // Change splash icon.
            if (ChangeSplashIcon == true) {
                // Some resources have been removed in the latest YouTube Music.
                // For compatibility, use try...catch.
                try {
                    splashIconResourceGroups.let { resourceGroups ->
                        resourceGroups.forEach {
                            context.copyResources("$appIconResourcePath/splash", it)
                        }
                    }
                } catch (_: Exception) {
                }
            }

            setIconType(appIcon)
        }
    }
}
