package app.morphe.patches.youtube.utils.fix.splash

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.youtube.utils.playservice.is_19_32_or_greater
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import app.morphe.patches.youtube.utils.settings.ResourceUtils.restoreOldSplashAnimationIncluded
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

    finalize {
        if (!is_19_32_or_greater) {
            return@finalize
        }

        if (restoreOldSplashAnimationIncluded) {
            document("res/values-night/styles.xml").use { document ->
                val resourcesNode = document.getElementsByTagName("resources").item(0) as Element
                val childNodes = resourcesNode.childNodes

                for (i in 0 until childNodes.length) {
                    val node = childNodes.item(i) as? Element ?: continue
                    val nodeAttributeName = node.getAttribute("name")
                    if (nodeAttributeName.startsWith("Theme.YouTube.Launcher")) {
                        val nodeAttributeParent = node.getAttribute("parent")

                        val style = document.createElement("style")
                        style.setAttribute("name", "Theme.YouTube.Home")
                        style.setAttribute("parent", nodeAttributeParent)

                        val windowItem = document.createElement("item")
                        windowItem.setAttribute("name", "android:windowBackground")
                        windowItem.textContent = "@color/yt_black1"
                        style.appendChild(windowItem)

                        resourcesNode.removeChild(node)
                        resourcesNode.appendChild(style)
                    }
                }
            }
        } else {
            document("res/values-night-v27/styles.xml").use { document ->
                // Create a night mode specific override for the splash screen background.
                val style = document.createElement("style")
                style.setAttribute("name", "Theme.YouTube.Home")
                style.setAttribute("parent", "@style/Base.V27.Theme.YouTube.Home")

                // Fix status and navigation bar showing white on some Android devices,
                // such as SDK 28 Android 10 medium tablet.
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
