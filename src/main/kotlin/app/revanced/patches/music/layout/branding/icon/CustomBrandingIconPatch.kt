package app.revanced.patches.music.layout.branding.icon

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.booleanPatchOption
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.stringPatchOption
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.util.ResourceGroup
import app.revanced.util.copyResources
import app.revanced.util.patch.BaseResourcePatch
import java.io.File
import java.nio.file.Files

@Suppress("DEPRECATION", "unused")
object CustomBrandingIconPatch : BaseResourcePatch(
    name = "Custom branding icon YouTube Music",
    description = "Changes the YouTube Music app icon to the icon specified in options.json.",
    compatiblePackages = COMPATIBLE_PACKAGE
) {
    private const val DEFAULT_ICON_KEY = "Revancify Blue"

    private val availableIcon = mapOf(
        "MMT" to "mmt",
        DEFAULT_ICON_KEY to "revancify_blue",
        "Revancify Red" to "revancify_red",
        "Revancify Yellow" to "revancify_yellow",
        "AFN Blue" to "afn_blue",
        "AFN Red" to "afn_red",
        "Vanced Black" to "vanced_black",
        "Vanced Light" to "vanced_light"
    )

    private val SplashHeaderIcon by booleanPatchOption(
        key = "SplashHeaderIcon",
        default = true,
        title = "Splash and header icons",
        description = "Apply custom branding icon to Splash and Header.",
        required = true
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

    public var AppIcon by stringPatchOption(
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
                    } catch (_: Exception) {
                        throw PatchException("Invalid app icon path: $appIcon")
                    }
                }
            } else {
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

                // change resource icons.
                if (SplashHeaderIcon == true) {
                    try {
                        arrayOf(
                            ResourceGroup(
                                "drawable-hdpi",
                                "action_bar_logo_release.png",
                                "action_bar_logo.png",
                                "logo_music.png", // 6.32 and earlier
                                "ytm_logo.png", // 6.33 and later
                                "record.png",
                            ),

                            ResourceGroup(
                                "drawable-large-hdpi",
                                "record.png",
                            ),

                            ResourceGroup(
                                "drawable-large-mdpi",
                                "record.png",
                            ),

                            ResourceGroup(
                                "drawable-large-xhdpi",
                                "record.png",
                            ),

                            ResourceGroup(
                                "drawable-xlarge-hdpi",
                                "record.png",
                            ),

                            ResourceGroup(
                                "drawable-xlarge-mdpi",
                                "record.png",
                            ),

                            ResourceGroup(
                                "drawable-mdpi",
                                "action_bar_logo.png",
                                "logo_music.png", // 6.32 and earlier
                                "ytm_logo.png", // 6.33 and later
                                "record.png",
                            ),

                            ResourceGroup(
                                "drawable-xhdpi",
                                "action_bar_logo.png",
                                "logo_music.png", // 6.32 and earlier
                                "ytm_logo.png", // 6.33 and later
                                "record.png",
                            ),

                            ResourceGroup(
                                "drawable-xxhdpi",
                                "action_bar_logo.png",
                                "logo_music.png", // 6.32 and earlier
                                "ytm_logo.png", // 6.33 and later
                                "record.png",
                            ),


                            ResourceGroup(
                                "drawable-xxxhdpi",
                                "action_bar_logo.png",
                                "logo_music.png", // 6.32 and earlier
                                "ytm_logo.png", // 6.33 and later
                            ),
                        ).forEach { resourceGroup ->
                            context.copyResources("$resourcePath/resource", resourceGroup)
                        }
                    } catch (e: Exception) {
                        // Do nothing
                    }
                }
            }
        } ?: throw PatchException("Invalid app icon path.")
    }
}