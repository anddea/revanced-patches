package app.revanced.patches.youtube.layout.theme

import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.shared.drawable.addDrawableColorHook
import app.revanced.patches.shared.drawable.drawableColorHookPatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.extension.Constants.UTILS_PATH
import app.revanced.patches.youtube.utils.playservice.is_19_32_or_greater
import app.revanced.patches.youtube.utils.playservice.versionCheckPatch
import org.w3c.dom.Element

private const val SPLASH_SCREEN_COLOR_NAME = "splashScreenColor"
private const val SPLASH_SCREEN_COLOR_ATTRIBUTE = "?attr/$SPLASH_SCREEN_COLOR_NAME"

val sharedThemePatch = resourcePatch(
    description = "sharedThemePatch"
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        drawableColorHookPatch,
        versionCheckPatch,
    )

    execute {
        addDrawableColorHook("$UTILS_PATH/DrawableColorPatch;->getLithoColor(I)I")

        // edit the resource files to change the splash screen color
        val attrsResourceFile = "res/values/attrs.xml"

        document(attrsResourceFile).use { document ->
            (document.getElementsByTagName("resources").item(0) as Element).appendChild(
                document.createElement("attr").apply {
                    setAttribute("format", "reference")
                    setAttribute("name", SPLASH_SCREEN_COLOR_NAME)
                }
            )
        }

        setOf(
            "res/values/styles.xml",
            "res/values-v31/styles.xml"
        ).forEachIndexed { pathIndex, stylesPath ->
            document(stylesPath).use { document ->
                val childNodes =
                    (document.getElementsByTagName("resources").item(0) as Element).childNodes

                for (i in 0 until childNodes.length) {
                    val node = childNodes.item(i) as? Element ?: continue
                    val nodeAttributeName = node.getAttribute("name")

                    document.createElement("item").apply {
                        setAttribute(
                            "name",
                            when (pathIndex) {
                                0 -> "splashScreenColor"
                                1 -> "android:windowSplashScreenBackground"
                                else -> "null"
                            }
                        )

                        appendChild(
                            document.createTextNode(
                                when (pathIndex) {
                                    0 -> when (nodeAttributeName) {
                                        "Base.Theme.YouTube.Launcher.Dark" -> "@color/yt_black1"
                                        "Base.Theme.YouTube.Launcher.Light" -> "@color/yt_white1"
                                        else -> "null"
                                    }

                                    1 -> when (nodeAttributeName) {
                                        "Base.Theme.YouTube.Launcher" -> SPLASH_SCREEN_COLOR_ATTRIBUTE
                                        else -> "null"
                                    }

                                    else -> "null"
                                }
                            )
                        )

                        if (this.textContent != "null")
                            node.appendChild(this)
                    }
                }
            }
        }

        setOf(
            "res/drawable/quantum_launchscreen_youtube.xml",
            "res/drawable-sw600dp/quantum_launchscreen_youtube.xml"
        ).forEach editSplashScreen@{ resourceFile ->
            document(resourceFile).use { document ->
                val layerList = document.getElementsByTagName("layer-list").item(0) as Element

                val childNodes = layerList.childNodes
                for (i in 0 until childNodes.length) {
                    val node = childNodes.item(i)
                    if (node is Element && node.hasAttribute("android:drawable")) {
                        node.setAttribute("android:drawable", SPLASH_SCREEN_COLOR_ATTRIBUTE)
                        return@editSplashScreen
                    }
                }

                throw PatchException("Failed to modify launch screen")
            }
        }

        if (is_19_32_or_greater) {
            // Fix the splash screen dark mode background color.
            // In earlier versions of the app this is white and makes no sense for dark mode.
            // This is only required for 19.32 and greater, but is applied to all targets.
            // Only dark mode needs this fix as light mode correctly uses the custom color.
            document("res/values-night/styles.xml").use { document ->
                val resourcesNode = document.getElementsByTagName("resources").item(0) as Element
                val childNodes = resourcesNode.childNodes

                for (i in 0 until childNodes.length) {
                    val node = childNodes.item(i) as? Element ?: continue
                    val nodeAttributeName = node.getAttribute("name")
                    if (nodeAttributeName == "Theme.YouTube.Launcher" || nodeAttributeName == "Theme.YouTube.Launcher.Cairo") {
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
}
