package app.revanced.util.resources

import app.revanced.extensions.doRecursively
import app.revanced.patcher.data.ResourceContext
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.nio.file.Files
import java.nio.file.StandardCopyOption

private fun Node.insertNode(tagName: String, targetNode: Node, block: Element.() -> Unit) {
    val child = ownerDocument.createElement(tagName)
    child.block()
    parentNode.insertBefore(child, targetNode)
}


internal object ResourceHelper {

    private const val TARGET_ARRAY_PATH = "res/values/arrays.xml"

    private const val TARGET_PREFERENCE_PATH = "res/xml/revanced_prefs.xml"

    private const val YOUTUBE_SETTINGS_PATH = "res/xml/settings_fragment.xml"

    private var targetPackage = "com.google.android.youtube"

    internal fun setMicroG (newPackage: String) {
        targetPackage = newPackage
    }

    internal fun ResourceContext.addSpeedEntryValues(speedEntryValues: String) {
        xmlEditor[TARGET_ARRAY_PATH].use {
            with(it.file) {
                val resourcesNode = getElementsByTagName("resources").item(0) as Element

                val newElement: Element = createElement("item")

                for (i in 0 until resourcesNode.childNodes.length) {
                    val node = resourcesNode.childNodes.item(i) as? Element ?: continue

                    if (node.getAttribute("name") == "revanced_custom_video_speed_entry_value") {
                        newElement.appendChild(createTextNode(speedEntryValues))

                        node.appendChild(newElement)
                    }
                }
            }
        }
    }

    internal fun ResourceContext.addSpeedEntries(speedEntries: String) {
        xmlEditor[TARGET_ARRAY_PATH].use {
            with(it.file) {
                val resourcesNode = getElementsByTagName("resources").item(0) as Element

                val newElement: Element = createElement("item")

                for (i in 0 until resourcesNode.childNodes.length) {
                    val node = resourcesNode.childNodes.item(i) as? Element ?: continue

                    if (node.getAttribute("name") == "revanced_custom_video_speed_entry") {
                        newElement.appendChild(createTextNode(speedEntries))

                        node.appendChild(newElement)
                    }
                }
            }
        }
        this[TARGET_ARRAY_PATH].writeText(
            this[TARGET_ARRAY_PATH].readText().replace("1.0x", "@string/shorts_speed_control_normal_label")
        )
    }

    internal fun ResourceContext.addPreference(settingArray: Array<String>) {
        val prefs = this[TARGET_PREFERENCE_PATH]

        settingArray.forEach preferenceLoop@{ preference ->
            prefs.writeText(
                prefs.readText()
                    .replace("<!-- $preference", "")
                    .replace("$preference -->", "")
            )
        }
    }

    internal fun ResourceContext.updatePatchStatus(patchTitle: String) {
        updatePatchStatusSettings(patchTitle, "@string/revanced_patches_included")
    }

    internal fun ResourceContext.updatePatchStatusLabel(appName: String) {
        updatePatchStatusSettings("labels", appName)
    }

    internal fun ResourceContext.updatePatchStatusIcon(iconName: String) {
        updatePatchStatusSettings("icons", "@string/revanced_icons_$iconName")
    }

    internal fun ResourceContext.updatePatchStatusTheme(themeName: String) {
        updatePatchStatusSettings("themes", "@string/revanced_themes_$themeName")
    }

    private fun ResourceContext.updatePatchStatusSettings(
        patchTitle: String,
        updateText: String
    ) {
        this.xmlEditor[TARGET_PREFERENCE_PATH].use { editor ->
            editor.file.doRecursively loop@{
                if (it !is Element) return@loop

                it.getAttributeNode("android:title")?.let { attribute ->
                    if (attribute.textContent == patchTitle) {
                        it.getAttributeNode("android:summary").textContent = updateText
                    }
                }
            }
        }
    }

    internal fun ResourceContext.addReVancedPreference(key: String) {
        val targetClass = "com.google.android.apps.youtube.app.settings.videoquality.VideoQualitySettingsActivity"

        this.xmlEditor[YOUTUBE_SETTINGS_PATH].use { editor ->
            with (editor.file) {
                doRecursively loop@{
                    if (it !is Element) return@loop
                    it.getAttributeNode("android:key")?.let { attribute ->
                        if (attribute.textContent == "@string/about_key" && it.getAttributeNode("app:iconSpaceReserved").textContent == "false") {
                            it.insertNode("Preference", it) {
                                setAttribute("android:title", "@string/revanced_" + key + "_title")
                                setAttribute("android:summary", "@string/revanced_" + key + "_summary")
                                this.appendChild(ownerDocument.createElement("intent").also { intentNode ->
                                    intentNode.setAttribute("android:targetPackage", targetPackage)
                                    intentNode.setAttribute("android:data", key)
                                    intentNode.setAttribute("android:targetClass", targetClass)
                                })
                            }
                            it.getAttributeNode("app:iconSpaceReserved").textContent = "true"
                            return@loop
                        }
                    }
                }

                doRecursively loop@{
                    if (it !is Element) return@loop

                    it.getAttributeNode("app:iconSpaceReserved")?.let { attribute ->
                        if (attribute.textContent == "true") {
                            attribute.textContent = "false"
                        }
                    }
                }
            }
        }
    }

    internal fun ResourceContext.addTranslations(
        sourceDirectory: String,
        languageArray: Array<String>
    ) {
        languageArray.forEach { language ->
            val directory = "values-$language-v21"
            val relativePath = "$language/strings.xml"

            this["res/$directory"].mkdir()

            Files.copy(
                ResourceUtils.javaClass.classLoader.getResourceAsStream("$sourceDirectory/translations/$relativePath")!!,
                this["res"].resolve("$directory/strings.xml").toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }
}