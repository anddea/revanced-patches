package app.revanced.patches.reddit.utils.settings.resource.patch

import app.revanced.extensions.doRecursively
import app.revanced.extensions.startsWithAny
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patches.shared.patch.settings.AbstractSettingsResourcePatch
import org.w3c.dom.Element
import kotlin.io.path.exists

@Name("settings-resource-patch")
@Version("0.0.1")
class SettingsResourcePatch : AbstractSettingsResourcePatch(
    "reddit/settings",
    "reddit/settings/host",
    false
) {
    override fun execute(context: ResourceContext): PatchResult {
        super.execute(context)

        fun setIcon(targetXML: String) {
            context.xmlEditor["res/xml/$targetXML.xml"].use { editor ->
                editor.file.doRecursively loop@{
                    if (it !is Element) return@loop

                    it.getAttributeNode("android:key")?.let { attribute ->
                        if (attribute.textContent.endsWith("key_pref_acknowledgements")) {
                            it.getAttributeNode("android:icon").textContent =
                                "@drawable/icon_beta_planet"
                        }
                    }
                }
            }
        }

        arrayOf("preferences", "preferences_logged_in").forEach { targetXML ->
            val resDirectory = context["res"]
            val targetXml = resDirectory.resolve("xml").resolve("$targetXML.xml").toPath()

            if (targetXml.exists())
                setIcon(targetXML)
        }

        // App name
        val resourceFileNames = arrayOf("strings.xml")

        context.forEach {
            if (!it.name.startsWithAny(*resourceFileNames)) return@forEach

            // for each file in the "layouts" directory replace all necessary attributes content
            context.xmlEditor[it.absolutePath].use { editor ->
                val resourcesNode = editor.file.getElementsByTagName("resources").item(0) as Element

                for (i in 0 until resourcesNode.childNodes.length) {
                    val node = resourcesNode.childNodes.item(i)
                    if (node !is Element) continue

                    val element = resourcesNode.childNodes.item(i) as Element
                    element.textContent = when (element.getAttribute("name")) {
                        "label_acknowledgements" -> "@string/revanced_extended_settings_title"
                        else -> continue
                    }
                }
            }
        }

        return PatchResultSuccess()
    }
}