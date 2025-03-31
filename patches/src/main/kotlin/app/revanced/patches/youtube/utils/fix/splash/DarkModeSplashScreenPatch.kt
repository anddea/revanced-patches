package app.revanced.patches.youtube.utils.fix.splash

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.utils.compatibility.Constants.YOUTUBE_PACKAGE_NAME
import app.revanced.patches.youtube.utils.playservice.is_19_32_or_greater
import app.revanced.patches.youtube.utils.playservice.versionCheckPatch
import app.revanced.patches.youtube.utils.settings.ResourceUtils.youtubePackageName
import app.revanced.util.findElementByAttributeValueOrThrow
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
            "values-night",
            "values-night-v27",
        ).forEach { directory ->
            document("res/$directory/styles.xml").use { document ->
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

                        resourcesNode.removeChild(node)
                        resourcesNode.appendChild(style)
                    }
                }
            }
        }
    }

    finalize {
        // GmsCore support included
        if (youtubePackageName != YOUTUBE_PACKAGE_NAME) {
            document("AndroidManifest.xml").use { document ->
                val mainActivityElement = document.childNodes.findElementByAttributeValueOrThrow(
                    "android:name",
                    "com.google.android.apps.youtube.app.watchwhile.MainActivity",
                )

                mainActivityElement.setAttribute("android:launchMode", "singleTask")
            }
        }
    }
}
