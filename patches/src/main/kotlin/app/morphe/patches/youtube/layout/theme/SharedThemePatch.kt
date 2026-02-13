package app.morphe.patches.youtube.layout.theme

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.drawable.addDrawableColorHook
import app.morphe.patches.shared.drawable.drawableColorHookPatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.youtube.utils.extension.Constants.UTILS_PATH
import org.w3c.dom.Element

private const val SPLASH_SCREEN_COLOR_NAME = "splashScreenColor"
private const val SPLASH_SCREEN_COLOR_ATTRIBUTE = "?attr/$SPLASH_SCREEN_COLOR_NAME"

val sharedThemePatch = resourcePatch(
    description = "sharedThemePatch"
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(drawableColorHookPatch)

    execute {
        addDrawableColorHook("$UTILS_PATH/DrawableColorPatch;->getLithoColor(I)I")

        // edit the resource files to change the splash screen color
        val attrsResourceFile = "res/values/attrs.xml"

        document(attrsResourceFile).use { document ->
            (document.getElementsByTagName("resources").item(0) as Element).appendChild(
                document.createElement("attr").apply {
                    setAttribute("format", "reference|color")
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
                                0 -> SPLASH_SCREEN_COLOR_NAME
                                1 -> "android:windowSplashScreenBackground"
                                else -> "null"
                            }
                        )

                        appendChild(
                            document.createTextNode(
                                when (pathIndex) {
                                    0 -> when (nodeAttributeName) {
                                        "Base.Theme.YouTube.Launcher.Dark",
                                        "Base.Theme.YouTube.Launcher.Cairo.Dark" -> "@color/yt_black1"

                                        "Base.Theme.YouTube.Launcher.Light",
                                        "Base.Theme.YouTube.Launcher.Cairo.Light" -> "@color/yt_white1"

                                        else -> "null"
                                    }

                                    1 -> when (nodeAttributeName) {
                                        "Base.Theme.YouTube.Launcher",
                                        "Base.Theme.YouTube.Launcher.Cairo" -> SPLASH_SCREEN_COLOR_ATTRIBUTE

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

        var launchScreenArray = emptyArray<String>()

        document("res/values/styles.xml").use { document ->
            val resourcesNode = document.getElementsByTagName("resources").item(0) as Element
            val childNodes = resourcesNode.childNodes

            for (i in 0 until childNodes.length) {
                val node = childNodes.item(i) as? Element ?: continue
                val nodeAttributeName = node.getAttribute("name")
                if (nodeAttributeName.startsWith("Base.Theme.YouTube.Launcher")) {
                    val itemNodes = node.childNodes

                    for (j in 0 until itemNodes.length) {
                        val item = itemNodes.item(j) as? Element ?: continue

                        val itemAttributeName = item.getAttribute("name")
                        if (itemAttributeName == "android:windowBackground" && item.textContent != null) {
                            launchScreenArray += item.textContent.split("/")[1]
                        }
                    }
                }
            }
        }

        launchScreenArray
            .distinct()
            .forEach { fileName ->
                arrayOf("drawable", "drawable-sw600dp").forEach editSplashScreen@{ drawable ->
                    val targetXmlPath = get("res").resolve(drawable).resolve("$fileName.xml")
                    if (!targetXmlPath.exists()) {
                        return@editSplashScreen
                    }
                    document("res/$drawable/$fileName.xml").use { document ->
                        val layerList =
                            document.getElementsByTagName("layer-list").item(0) as Element

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
            }

    }
}
