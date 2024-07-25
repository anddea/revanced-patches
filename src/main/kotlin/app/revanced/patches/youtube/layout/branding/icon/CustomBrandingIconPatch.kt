package app.revanced.patches.youtube.layout.branding.icon

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.booleanPatchOption
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.stringPatchOption
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.settings.ResourceUtils.updatePatchStatusIcon
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.ResourceGroup
import app.revanced.util.Utils.trimIndentMultiline
import app.revanced.util.copyFile
import app.revanced.util.copyResources
import app.revanced.util.copyXmlNode
import app.revanced.util.patch.BaseResourcePatch
import app.revanced.util.underBarOrThrow

@Suppress("unused")
object CustomBrandingIconPatch : BaseResourcePatch(
    name = "Custom branding icon for YouTube",
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
        "MMT Blue" to "mmt_blue",
        "MMT Green" to "mmt_green",
        "MMT Yellow" to "mmt_yellow",
        DEFAULT_ICON_KEY to "revancify_blue",
        "Revancify Red" to "revancify_red",
        "Revancify Yellow" to "revancify_yellow",
        "Vanced Black" to "vanced_black",
        "Vanced Light" to "vanced_light",
        "YouTube" to "youtube"
    )

    private val sizeArray = arrayOf(
        "xxxhdpi",
        "xxhdpi",
        "xhdpi",
        "hdpi",
        "mdpi"
    )

    private val drawableDirectories = sizeArray.map { "drawable-$it" }

    private val mipmapDirectories = sizeArray.map { "mipmap-$it" }

    private val headerResourceDirectoryNames = mapOf(
        "xxxhdpi" to "512px x 192px",
        "xxhdpi" to "387px x 144px",
        "xhdpi" to "258px x 96px",
        "hdpi" to "194px x 72px",
        "mdpi" to "129px x 48px",
    ).map { (dpi, dim) ->
        "drawable-$dpi" to dim
    }.toMap()

    private val variants = arrayOf("light", "dark")

    private val headerIconResourceFileNames = arrayOf(
        "yt_premium_wordmark_header_dark",
        "yt_premium_wordmark_header_light",
        "yt_wordmark_header_dark",
        "yt_wordmark_header_light"
    ).map { "$it.png" }.toTypedArray()

    private val launcherIconResourceFileNames = arrayOf(
        "adaptiveproduct_youtube_background_color_108",
        "adaptiveproduct_youtube_foreground_color_108",
        "ic_launcher",
        "ic_launcher_round"
    ).map { "$it.png" }.toTypedArray()

    private val splashIconResourceFileNames = arrayOf(
        "product_logo_youtube_color_24",
        "product_logo_youtube_color_36",
        "product_logo_youtube_color_144",
        "product_logo_youtube_color_192"
    ).map { "$it.png" }.toTypedArray()

    private val oldSplashAnimationResourceFileNames = arrayOf(
        "\$\$avd_anim__1__0",
        "\$\$avd_anim__1__1",
        "\$\$avd_anim__2__0",
        "\$\$avd_anim__2__1",
        "\$\$avd_anim__3__0",
        "\$\$avd_anim__3__1",
        "\$avd_anim__0",
        "\$avd_anim__1",
        "\$avd_anim__2",
        "\$avd_anim__3",
        "\$avd_anim__4",
        "avd_anim"
    ).map { "$it.xml" }.toTypedArray()

    private fun List<String>.getResourceGroup(fileNames: Array<String>) = map { directory ->
        ResourceGroup(
            directory, *fileNames
        )
    }

    private val headerIconResourceGroups =
        drawableDirectories.getResourceGroup(headerIconResourceFileNames)

    private val launcherIconResourceGroups =
        mipmapDirectories.getResourceGroup(launcherIconResourceFileNames)

    private val splashIconResourceGroups =
        drawableDirectories.getResourceGroup(splashIconResourceFileNames)

    private val oldSplashAnimationResourceGroups =
        listOf("drawable").getResourceGroup(oldSplashAnimationResourceFileNames)

    // region patch option

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

    private val CustomHeader by stringPatchOption(
        key = "CustomHeader",
        default = "",
        title = "Custom header",
        description = """
            The header to apply to the app.
            
            If a path to a folder is provided, the folder must contain one or more of the following folders, depending on the DPI of the device:
            
            ${headerResourceDirectoryNames.keys.joinToString("\n") { "- $it" }}
            
            Each of the folders must contain all of the following files:
            
            ${headerIconResourceFileNames.joinToString("\n") { "- $it" }}

            The image dimensions must be as follows:
            ${headerResourceDirectoryNames.map { (dpi, dim) -> "- $dpi: $dim" }.joinToString("\n")}
        """.trimIndentMultiline(),
        required = true
    )

    private val ChangeSplashIcon by booleanPatchOption(
        key = "ChangeSplashIcon",
        default = true,
        title = "Change splash icons",
        description = "Apply the custom branding icon to the splash screen.",
        required = true
    )

    private val RestoreOldSplashAnimation by booleanPatchOption(
        key = "RestoreOldSplashAnimation",
        default = true,
        title = "Restore old splash animation",
        description = "Restores old style splash animation.",
        required = true
    )

    // endregion

    override fun execute(context: ResourceContext) {

        // Check patch options first.
        val appIcon = AppIcon
            .underBarOrThrow()

        val appIconResourcePath = "youtube/branding/$appIcon"
        val stockResourcePath = "youtube/branding/stock"

        // Check if a custom path is used in the patch options.
        if (!availableIcon.containsValue(appIcon)) {
            val copiedFiles = context.copyFile(
                launcherIconResourceGroups,
                appIcon,
                "WARNING: Invalid app icon path: $appIcon. Does not apply patches."
            )
            if (copiedFiles)
                context.updatePatchStatusIcon("custom")
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
                    "adaptive_monochrome_ic_youtube_launcher.xml"
                )
            ).forEach { resourceGroup ->
                context.copyResources("$appIconResourcePath/monochrome", resourceGroup)
            }

            // Change header.
            if (ChangeHeader == true) {
                CustomHeader?.let { customHeader ->
                    var copiedFiles = false
                    if (customHeader.isNotEmpty()) {
                        copiedFiles = context.copyFile(
                            headerIconResourceGroups,
                            customHeader,
                            "WARNING: Invalid header path: $customHeader. Does not apply patches."
                        )
                    }
                    if (!copiedFiles) {
                        headerIconResourceGroups.let { resourceGroups ->
                            resourceGroups.forEach {
                                context.copyResources("$appIconResourcePath/header", it)
                            }
                        }
                    }
                }
            }

            // Change splash icon.
            if (ChangeSplashIcon == true) {
                splashIconResourceGroups.let { resourceGroups ->
                    resourceGroups.forEach {
                        context.copyResources("$appIconResourcePath/splash", it)
                    }
                }
            }

            // Change splash screen.
            if (RestoreOldSplashAnimation == true) {
                oldSplashAnimationResourceGroups.let { resourceGroups ->
                    resourceGroups.forEach {
                        context.copyResources("$appIconResourcePath/splash", it)
                    }
                }

                context.copyXmlNode(
                    "$stockResourcePath/splash",
                    "values-v31/styles.xml",
                    "resources"
                )
            }

            context.updatePatchStatusIcon(appIcon)
        }
    }
}
