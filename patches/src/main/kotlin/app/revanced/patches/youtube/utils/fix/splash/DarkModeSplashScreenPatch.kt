package app.revanced.patches.youtube.utils.fix.splash

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.utils.playservice.is_19_32_or_greater
import app.revanced.patches.youtube.utils.playservice.versionCheckPatch
import org.w3c.dom.Element

/**
 * Fix the splash screen dark mode background color.
 * In earlier versions of the app this is white and makes no sense for dark mode.
 * This is only required for 19.32 and greater, but is applied to all targets.
 * Only dark mode needs this fix as light mode correctly uses the custom color.
 *
 * This is a bug in unpatched YouTube.
 * Should always be applied even if the `Theme` patch is excluded.
 */
val darkModeSplashScreenPatch = resourcePatch(
    description = "darkModeSplashScreenPatch"
) {
    dependsOn(versionCheckPatch)

    execute {
        if (!is_19_32_or_greater) {
            return@execute
        }

        arrayOf(
            "values-night" to "@style/Base.V23.Theme.YouTube.Home",
            "values-night-v27" to "@style/Base.V27.Theme.YouTube.Home",
        ).forEach { (directory, parent) ->
            document("res/$directory/styles.xml").use { document ->
                // Create a night mode specific override for the splash screen background.
                val style = document.createElement("style")
                style.setAttribute("name", "Theme.YouTube.Home")
                style.setAttribute("parent", parent)

                val colorSplashBackgroundColor = "@color/yt_black1"
                arrayOf(
                    "android:navigationBarColor" to colorSplashBackgroundColor,
                    "android:windowBackground" to colorSplashBackgroundColor,
                    "android:colorBackground" to colorSplashBackgroundColor,
                    "colorPrimaryDark" to colorSplashBackgroundColor,
                    "android:windowLightStatusBar" to "false",
                ).forEach { (name, value) ->
                    val styleItem = document.createElement("item")
                    styleItem.setAttribute("name", name)
                    styleItem.textContent = value
                    style.appendChild(styleItem)
                }

                val resourcesNode = document.getElementsByTagName("resources").item(0) as Element
                resourcesNode.appendChild(style)
            }
        }
    }
}
