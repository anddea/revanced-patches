package app.revanced.patches.music.general.branding.icon

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.stringPatchOption
import app.revanced.util.ResourceGroup
import app.revanced.util.copyResources
import java.io.File
import java.nio.file.Files

@Patch(
    name = "Custom branding icon YouTube Music",
    description = "Change the YouTube Music launcher icon to the icon specified in options.json.",
    compatiblePackages = [CompatiblePackage("com.google.android.apps.youtube.music")]
)
@Suppress("unused")
object CustomBrandingIconPatch : ResourcePatch() {
    private const val DEFAULT_ICON_KEY = "Revancify Blue"

    private val availableIcon = mapOf(
        "MMT" to "mmt",
        DEFAULT_ICON_KEY to "revancify_blue",
        "Revancify Red" to "revancify_red"
    )

    private val mipmapIconResourceFileNames = arrayOf(
        "adaptiveproduct_youtube_music_background_color_108",
        "adaptiveproduct_youtube_music_foreground_color_108",
        "ic_launcher_release"
    ).map { "$it.png" }.toTypedArray()

    private val mipmapDirectories = arrayOf(
        "xxxhdpi",
        "xxhdpi",
        "xhdpi",
        "hdpi",
        "mdpi"
    ).map { "mipmap-$it" }

    private var AppIcon by stringPatchOption(
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
        required = true
    )

    override fun execute(context: ResourceContext) {
        AppIcon?.let { appIcon ->
            if (!availableIcon.containsKey(appIcon)) {
                mipmapDirectories.map { directory ->
                    ResourceGroup(
                        directory, *mipmapIconResourceFileNames
                    )
                }.let { resourceGroups ->
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
                }
            } else {
                val appIconValue = availableIcon[appIcon] + ""
                val resourcePath = "music/branding/$appIconValue"

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

                // change monochrome icon.
                arrayOf(
                    ResourceGroup(
                        "drawable",
                        "ic_app_icons_themed_youtube_music.xml"
                    )
                ).forEach { resourceGroup ->
                    context.copyResources("$resourcePath/monochrome", resourceGroup)
                }
            }
        } ?: throw PatchException("Invalid app icon path.")
    }
}