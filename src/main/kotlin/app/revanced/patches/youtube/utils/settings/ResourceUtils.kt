package app.revanced.patches.youtube.utils.settings

import app.revanced.patcher.data.ResourceContext
import app.revanced.util.doRecursively
import app.revanced.util.insertNode
import org.w3c.dom.Element

@Suppress("MemberVisibilityCanBePrivate")
object ResourceUtils {

    const val TARGET_PREFERENCE_PATH = "res/xml/revanced_prefs.xml"

    const val YOUTUBE_SETTINGS_PATH = "res/xml/settings_fragment.xml"

    var targetPackage = "com.google.android.youtube"

    fun setMicroG(newPackage: String) {
        targetPackage = newPackage
    }

    fun ResourceContext.addEntryValues(
        path: String,
        speedEntryValues: String,
        attributeName: String
    ) {
        xmlEditor[path].use {
            with(it.file) {
                val resourcesNode = getElementsByTagName("resources").item(0) as Element

                val newElement: Element = createElement("item")

                for (i in 0 until resourcesNode.childNodes.length) {
                    val node = resourcesNode.childNodes.item(i) as? Element ?: continue

                    if (node.getAttribute("name") == attributeName) {
                        newElement.appendChild(createTextNode(speedEntryValues))

                        node.appendChild(newElement)
                    }
                }
            }
        }
    }

    fun ResourceContext.addPreference(settingArray: Array<String>) {
        val prefs = this[TARGET_PREFERENCE_PATH]

        settingArray.forEach preferenceLoop@{ preference ->
            prefs.writeText(
                prefs.readText()
                    .replace("<!-- $preference", "")
                    .replace("$preference -->", "")
            )
        }
    }

    fun ResourceContext.updatePatchStatus(patchTitle: String) {
        updatePatchStatusSettings(patchTitle, "@string/revanced_patches_included")
    }

    fun ResourceContext.updatePatchStatusHeader(headerName: String) {
        updatePatchStatusSettings("Custom branding heading", headerName)
    }

    fun ResourceContext.updatePatchStatusIcon(iconName: String) {
        updatePatchStatusSettings("Icon", "@string/revanced_icon_$iconName")
    }

    fun ResourceContext.updatePatchStatusLabel(appName: String) {
        updatePatchStatusSettings("Label", appName)
    }

    fun ResourceContext.updatePatchStatusTheme(themeName: String) {
        updatePatchStatusSettings("Theme", themeName)
    }

    fun ResourceContext.updatePatchStatusSettings(
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

    fun ResourceContext.addReVancedPreference(key: String, insertKey: String) {
        val targetClass = "com.google.android.apps.youtube.app.settings.videoquality.VideoQualitySettingsActivity"
        val path = if (key == "extended_settings") YOUTUBE_SETTINGS_PATH else TARGET_PREFERENCE_PATH

        this.xmlEditor[path].use { editor ->
            with(editor.file) {
                val processedKeys = mutableSetOf<String>() // To track processed keys

                doRecursively loop@{ node ->
                    if (node !is Element) return@loop // Skip if not an element

                    val attributeNode = node.getAttributeNode("android:key") ?: return@loop // Skip if no key attribute

                    val currentKey = attributeNode.textContent

                    // Check if the current key has already been processed
                    if (processedKeys.contains(currentKey)) {
                        return@loop // Skip if already processed
                    } else {
                        processedKeys.add(currentKey) // Add the current key to processedKeys
                    }

                    when {
                        key == "extended_settings" && currentKey == insertKey && node.getAttribute("app:iconSpaceReserved") == "false" -> {
                            node.insertNode("Preference", node) {
                                setAttribute("android:key", "revanced_${key}_key")
                                setAttribute("android:title", "@string/revanced_${key}_title")
                                this.appendChild(
                                    ownerDocument.createElement("intent").also { intentNode ->
                                        intentNode.setAttribute("android:targetPackage", targetPackage)
                                        intentNode.setAttribute("android:data", key)
                                        intentNode.setAttribute("android:targetClass", targetClass)
                                    }
                                )
                            }
                            node.setAttribute("app:iconSpaceReserved", "true")
                        }
                        key != "extended_settings" && currentKey == insertKey -> {
                            node.insertNode("PreferenceScreen", node) {
                                setAttribute("android:key", "revanced_${key}_key")
                                setAttribute("android:title", "@string/revanced_${key}_title")
                                this.appendChild(
                                    ownerDocument.createElement("intent").also { intentNode ->
                                        intentNode.setAttribute("android:targetPackage", targetPackage)
                                        intentNode.setAttribute("android:data", key)
                                        intentNode.setAttribute("android:targetClass", targetClass)
                                    }
                                )
                            }
                        }
                        currentKey == "true" -> {
                            attributeNode.textContent = "false"
                        }
                    }
                }
            }
        }
    }

}