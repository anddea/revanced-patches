package app.revanced.patches.youtube.layout.branding.icon

import app.revanced.patcher.patch.booleanOption
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patcher.patch.stringOption
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.patch.PatchList.CUSTOM_BRANDING_ICON_FOR_YOUTUBE
import app.revanced.patches.youtube.utils.playservice.is_19_17_or_greater
import app.revanced.patches.youtube.utils.playservice.is_19_34_or_greater
import app.revanced.patches.youtube.utils.playservice.versionCheckPatch
import app.revanced.patches.youtube.utils.settings.ResourceUtils.updatePatchStatusIcon
import app.revanced.patches.youtube.utils.settings.settingsPatch
import app.revanced.util.ResourceGroup
import app.revanced.util.Utils.trimIndentMultiline
import app.revanced.util.copyFile
import app.revanced.util.copyResources
import app.revanced.util.copyXmlNode
import app.revanced.util.getAdaptiveIconResourceFile
import app.revanced.util.getResourceGroup
import app.revanced.util.underBarOrThrow
import app.revanced.util.valueOrThrow
import java.nio.file.Files
import java.nio.file.StandardCopyOption

private const val ADAPTIVE_ICON_BACKGROUND_FILE_NAME =
    "adaptiveproduct_youtube_background_color_108"
private const val ADAPTIVE_ICON_FOREGROUND_FILE_NAME =
    "adaptiveproduct_youtube_foreground_color_108"
private const val DEFAULT_ICON = "xisr_yellow"

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
    "Xisr Yellow" to DEFAULT_ICON,
    "YouTube" to "youtube",
    "YouTube Black" to "youtube_black",
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

private val launcherIconResourceFileNames = arrayOf(
    ADAPTIVE_ICON_BACKGROUND_FILE_NAME,
    ADAPTIVE_ICON_FOREGROUND_FILE_NAME,
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

private val launcherIconResourceGroups =
    mipmapDirectories.getResourceGroup(launcherIconResourceFileNames)

private val splashIconResourceGroups =
    drawableDirectories.getResourceGroup(splashIconResourceFileNames)

private val oldSplashAnimationResourceGroups =
    listOf("drawable").getResourceGroup(oldSplashAnimationResourceFileNames)

@Suppress("unused")
val customBrandingIconPatch = resourcePatch(
    CUSTOM_BRANDING_ICON_FOR_YOUTUBE.title,
    CUSTOM_BRANDING_ICON_FOR_YOUTUBE.summary,
    false,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        versionCheckPatch,
    )


    val appIconOption = stringOption(
        key = "appIcon",
        default = DEFAULT_ICON,
        values = availableIcon,
        title = "App icon",
        description = """
            The icon to apply to the app.
            
            If a path to a folder is provided, the folder must contain the following folders:

            ${mipmapDirectories.joinToString("\n") { "- $it" }}

            Each of these folders must contain the following files:

            ${launcherIconResourceFileNames.joinToString("\n") { "- $it" }}
            """.trimIndentMultiline(),
        required = true,
    )

    val changeSplashIconOption by booleanOption(
        key = "changeSplashIcon",
        default = true,
        title = "Change splash icons",
        description = "Apply the custom branding icon to the splash screen.",
        required = true
    )

    val restoreOldSplashAnimationOption by booleanOption(
        key = "restoreOldSplashAnimation",
        default = true,
        title = "Restore old splash animation",
        description = "Restore the old style splash animation. Supports from YouTube 18.29.38 to YouTube 19.16.39.",
        required = true,
    )

    execute {
        // Check patch options first.
        var appIcon = appIconOption
            .underBarOrThrow()

        val appIconResourcePath = "youtube/branding/$appIcon"

        // Check if a custom path is used in the patch options.
        if (!availableIcon.containsValue(appIcon)) {
            appIcon = appIconOption.valueOrThrow()
            val copiedFiles = copyFile(
                launcherIconResourceGroups,
                appIcon,
                "WARNING: Invalid app icon path: $appIcon. Does not apply patches."
            )
            if (copiedFiles)
                updatePatchStatusIcon("custom")
        } else {
            // Change launcher icon.
            launcherIconResourceGroups.let { resourceGroups ->
                resourceGroups.forEach {
                    copyResources("$appIconResourcePath/launcher", it)
                }
            }

            // Change monochrome icon.
            arrayOf(
                ResourceGroup(
                    "drawable",
                    "adaptive_monochrome_ic_youtube_launcher.xml"
                )
            ).forEach { resourceGroup ->
                copyResources("$appIconResourcePath/monochrome", resourceGroup)
            }

            // Change splash icon.
            if (changeSplashIconOption == true) {
                splashIconResourceGroups.let { resourceGroups ->
                    resourceGroups.forEach {
                        copyResources("$appIconResourcePath/splash", it)
                    }
                }
            }

            // Change splash screen.
            if (restoreOldSplashAnimationOption == true) {
                if (!is_19_17_or_greater) {
                    oldSplashAnimationResourceGroups.let { resourceGroups ->
                        resourceGroups.forEach {
                            copyResources("$appIconResourcePath/splash", it)
                        }
                    }

                    copyXmlNode(
                        "$appIconResourcePath/splash",
                        "values-v31/styles.xml",
                        "resources"
                    )
                } else {
                    println("WARNING: \"Restore old splash animation\" is not supported in this version. Use YouTube 19.16.39 or earlier.")
                }
            }

            updatePatchStatusIcon(appIcon)
        }

        if (!is_19_34_or_greater) {
            return@execute
        }
        if (appIcon == "youtube") {
            return@execute
        }

        mapOf(
            ADAPTIVE_ICON_BACKGROUND_FILE_NAME to getAdaptiveIconResourceFile(
                "res/mipmap-anydpi/ic_launcher.xml",
                "background"
            ),
            ADAPTIVE_ICON_FOREGROUND_FILE_NAME to getAdaptiveIconResourceFile(
                "res/mipmap-anydpi/ic_launcher.xml",
                "foreground"
            )
        ).forEach { (oldIconResourceFile, newIconResourceFile) ->
            if (oldIconResourceFile != newIconResourceFile) {
                mipmapDirectories.forEach {
                    val mipmapDirectory = get("res").resolve(it)
                    Files.copy(
                        mipmapDirectory
                            .resolve("$oldIconResourceFile.png")
                            .toPath(),
                        mipmapDirectory
                            .resolve("$newIconResourceFile.png")
                            .toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                    )
                }
            }
        }
    }
}
