package app.revanced.shared.patches.theme.resource

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

        // copies the resource file to change the splash screen color
        context.xmlEditor["res/values/attrs.xml"].use { editor ->
            with(editor.file) {
                val resourcesNode = getElementsByTagName("resources").item(0) as Element

                val newElement: Element = createElement("attr")
                newElement.setAttribute("format", "reference")
                newElement.setAttribute("name", "splashScreenColor")

                resourcesNode.appendChild(newElement)
            }
        }

        context.xmlEditor["res/values/styles.xml"].use { editor ->
            with(editor.file) {
                val resourcesNode = getElementsByTagName("resources").item(0) as Element

                for (i in 0 until resourcesNode.childNodes.length) {
                    val node = resourcesNode.childNodes.item(i) as? Element ?: continue

                    val newElement: Element = createElement("item")
                    newElement.setAttribute("name", "splashScreenColor")

                    when (node.getAttribute("name")) {
                        "Base.Theme.YouTube.Launcher.Dark" -> {
                            newElement.appendChild(createTextNode("@color/yt_black1"))

                            node.appendChild(newElement)
                        }
                        "Base.Theme.YouTube.Launcher.Light" -> {
                            newElement.appendChild(createTextNode("@color/yt_white1"));

                            node.appendChild(newElement)
                        }
                    }
                }
            }
        }

        context.xmlEditor["res/values-v31/styles.xml"].use { editor ->
            with(editor.file) {
                val resourcesNode = getElementsByTagName("resources").item(0) as Element

                val newElement: Element = createElement("item")
                newElement.setAttribute("name", "android:windowSplashScreenBackground")

                for (i in 0 until resourcesNode.childNodes.length) {
                    val node = resourcesNode.childNodes.item(i) as? Element ?: continue

                    if (node.getAttribute("name") == "Base.Theme.YouTube.Launcher") {
                        newElement.appendChild(createTextNode("?attr/splashScreenColor"))

                        node.appendChild(newElement)
                    }
                }
            }
        }

        arrayOf("drawable", "drawable-sw600dp").forEach { drawablePath ->
            context.xmlEditor["res/$drawablePath/quantum_launchscreen_youtube.xml"].use { editor ->
                val resourcesNode = editor.file.getElementsByTagName("item").item(0) as Element

                if (resourcesNode.attributes.getNamedItem("android:drawable") != null)
                    resourcesNode.setAttribute("android:drawable", "?attr/splashScreenColor")
            }
        }

        return PatchResultSuccess()
    }
}