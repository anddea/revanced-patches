package app.revanced.patches.youtube.layout.theme

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.shared.patch.litho.LithoThemePatch
import app.revanced.patches.youtube.utils.integrations.Constants.UTILS_PATH
import org.w3c.dom.Element

@Patch(dependencies = [LithoThemePatch::class])
object GeneralThemePatch : ResourcePatch() {
    override fun execute(context: ResourceContext) {

        LithoThemePatch.injectCall("$UTILS_PATH/LithoThemePatch;->applyLithoTheme(I)I")

        // edit the resource files to change the splash screen color
        val attrsPath = "res/values/attrs.xml"
        val stylesPaths: List<String> = listOf(
            "res/values/styles.xml", // Android 11 (and below)
            "res/values-v31/styles.xml", // Android 12 (and above)
        )

        context.document[attrsPath].use { editor ->

            (editor.getElementsByTagName("resources").item(0) as Element)
                .appendChild(
                    editor.createElement("attr").apply {
                        setAttribute("format", "reference")
                        setAttribute("name", "splashScreenColor")
                    }
                )
        }
        stylesPaths.forEachIndexed { pathIndex, stylesPath ->
            context.document[stylesPath].use { editor ->

                val childNodes =
                    (editor.getElementsByTagName("resources").item(0) as Element).childNodes

                for (i in 0 until childNodes.length) {
                    val node = childNodes.item(i) as? Element ?: continue
                    val nodeAttributeName = node.getAttribute("name")

                    editor.createElement("item").apply {
                        setAttribute(
                            "name",
                            when (pathIndex) {
                                0 -> "splashScreenColor"
                                1 -> "android:windowSplashScreenBackground"
                                else -> "null"
                            }
                        )

                        appendChild(
                            editor.createTextNode(
                                when (pathIndex) {
                                    0 -> when (nodeAttributeName) {
                                        "Base.Theme.YouTube.Launcher.Dark" -> "@color/yt_black1"
                                        "Base.Theme.YouTube.Launcher.Light" -> "@color/yt_white1"
                                        else -> "null"
                                    }

                                    1 -> when (nodeAttributeName) {
                                        "Base.Theme.YouTube.Launcher" -> "?attr/splashScreenColor"
                                        else -> "null"
                                    }

                                    else -> "null"
                                }
                            )
                        )

                        if (this.textContent != "null") node.appendChild(this)
                    }
                }
            }
        }

        arrayOf("drawable", "drawable-sw600dp").forEach { quantumLaunchScreenPath ->
            context.document["res/$quantumLaunchScreenPath/quantum_launchscreen_youtube.xml"].use { editor ->
                val resourcesNode = editor.getElementsByTagName("item").item(0) as Element

                if (resourcesNode.attributes.getNamedItem("android:drawable") != null)
                    resourcesNode.setAttribute("android:drawable", "?attr/splashScreenColor")
            }
        }
    }

    internal var isMonetPatchIncluded: Boolean = false
}