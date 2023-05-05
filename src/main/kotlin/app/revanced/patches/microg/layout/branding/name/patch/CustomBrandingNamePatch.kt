package app.revanced.patches.microg.layout.branding.name.patch

import app.revanced.extensions.startsWithAny
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.*
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patches.shared.annotation.MicroGCompatibility
import org.w3c.dom.Element

@Patch
@Name("custom-branding-microg-name")
@Description("Changes the MicroG launcher name to your choice (defaults to MicroG).")
@MicroGCompatibility
@Version("0.0.1")
class CustomBrandingNamePatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        MicroGAppName?.let { appName ->
            val resourceFileNames = arrayOf("strings.xml")

            context.forEach {
                if (!it.name.startsWithAny(*resourceFileNames)) return@forEach

                context.xmlEditor[it.absolutePath].use { editor ->
                    val resourcesNode = editor.file.getElementsByTagName("resources").item(0) as Element
                    var label = ""

                    for (i in 0 until resourcesNode.childNodes.length) {
                        val node = resourcesNode.childNodes.item(i)
                        if (node !is Element) continue

                        val element = resourcesNode.childNodes.item(i) as Element

                        when (element.getAttribute("name")) {
                            "app_name", "gms_app_name" -> {
                                if (label == "")
                                    label = element.textContent

                                element.textContent = appName
                            }

                            "gms_settings_name"-> element.textContent = appName

                            else -> continue
                        }
                    }

                    for (i in 0 until resourcesNode.childNodes.length) {
                        val node = resourcesNode.childNodes.item(i)
                        if (node !is Element) continue

                        val element = resourcesNode.childNodes.item(i) as Element

                        if (element.textContent.contains(label)) {
                            element.textContent = element.textContent.replace(label, appName)
                        }

                    }
                }
            }

        }?: return PatchResultError("No app name provided")

        return PatchResultSuccess()
    }
    companion object : OptionsContainer() {
        var MicroGAppName: String? by option(
            PatchOption.StringOption(
                key = "MicroGAppName",
                default = "MicroG",
                title = "Application Name of MicroG",
                description = "The name of the MicroG it will show on your home screen."
            )
        )
    }
}
