package app.revanced.patches.youtube.utils.fix.splash

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.youtube.utils.playservice.is_19_32_or_greater
import app.revanced.patches.youtube.utils.playservice.versionCheckPatch
import org.w3c.dom.Element

val darkModeSplashScreenPatch = resourcePatch(
    description = "darkModeSplashScreenPatch"
) {
    dependsOn(versionCheckPatch)

    execute {
        if (!is_19_32_or_greater) {
            return@execute
        }

        /**
         * Fix the splash screen dark mode background color.
         * In earlier versions of the app this is white and makes no sense for dark mode.
         * This is only required for 19.32 and greater, but is applied to all targets.
         * Only dark mode needs this fix as light mode correctly uses the custom color.
         *
         * This is a bug in unpatched YouTube.
         * Should always be applied even if the `Theme` patch is excluded.
         */
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
    }
}
