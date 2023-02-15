package app.revanced.patches.youtube.layout.etc.theme.resource.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import org.w3c.dom.Element

@Name("general-theme-resource-patch")
@Version("0.0.1")
class GeneralThemeResourcePatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        // edit the resource files to change the splash screen color
        val attrsPath = "res/values/attrs.xml"
        val stylesPaths: List<String> = listOf(
            "res/values/styles.xml", // Android 11 (and below)
            "res/values-v31/styles.xml", // Android 12 (and above)
        )

        context.xmlEditor[attrsPath].use { editor ->
            val file = editor.file

            (file.getElementsByTagName("resources").item(0) as Element).appendChild(
                file.createElement("attr").apply {
                    setAttribute("format", "reference")
                    setAttribute("name", "splashScreenColor")
                }
            )
        }
        stylesPaths.forEachIndexed { pathIndex, stylesPath ->
            context.xmlEditor[stylesPath].use { editor ->
                val file = editor.file

                val childNodes = (file.getElementsByTagName("resources").item(0) as Element).childNodes

                for (i in 0 until childNodes.length) {
                    val node = childNodes.item(i) as? Element ?: continue
                    val nodeAttributeName = node.getAttribute("name")

                    file.createElement("item").apply {
                        setAttribute(
                            "name",
                            when (pathIndex) {
                                0 -> "splashScreenColor"
                                1 -> "android:windowSplashScreenBackground"
                                else -> "null"
                            }
                        )

                        appendChild(
                            file.createTextNode(
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

                        if (this.textContent != "null")
                            node.appendChild(this)
                    }
                }
            }
        }

        arrayOf("drawable", "drawable-sw600dp").forEach { quantumLaunchscreenPath ->
            context.xmlEditor["res/$quantumLaunchscreenPath/quantum_launchscreen_youtube.xml"].use { editor ->
                val resourcesNode = editor.file.getElementsByTagName("item").item(0) as Element

                if (resourcesNode.attributes.getNamedItem("android:drawable") != null)
                    resourcesNode.setAttribute("android:drawable", "?attr/splashScreenColor")
            }
        }

        return PatchResultSuccess()
    }
}