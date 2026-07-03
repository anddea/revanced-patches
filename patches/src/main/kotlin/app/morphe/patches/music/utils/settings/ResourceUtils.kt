/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
 * - ILoveOpenSourceApplications (https://github.com/ILoveOpenSourceApplications)
 * - inotia00 (https://github.com/inotia00)
 * - VazerOG (https://github.com/VazerOG)
 *
 * Licensed under the GNU General Public License v3.0.
 *
 * ------------------------------------------------------------------------
 * GPLv3 Section 7 – Additional Terms & Attribution Requirements
 * ------------------------------------------------------------------------
 *
 * This file contains substantial original work by the author(s) listed above.
 *
 * In accordance with Section 7 of the GNU General Public License v3.0,
 * the following additional terms apply to this file:
 *
 * 1. Source Credit Preservation (Section 7(b)): This specific copyright notice
 *    and the list of original authors above must be preserved in any copy
 *    or derivative work. You may add your own copyright notice below it,
 *    but you may not remove the original one.
 *
 * 2. Origin & Modification Marking (Section 7(c)): Modified versions must be
 *    clearly marked as such (e.g., by adding a "Modified by" line or a new
 *    copyright notice) and must not be misrepresented as the original work.
 *
 * 3. Version Control Attribution (Section 7(b)): Any ports or substantial
 *    modifications must retain historical authorship credit in version control
 *    systems (e.g., Git), listing original author(s) appropriately and
 *    modifiers as committers or co-authors.
 *
 * 4. User Interface Attribution (Section 7(b)): Any works containing or
 *    derived from this material must maintain a visible credit or
 *    acknowledgment to the original author(s) within the application's
 *    user interface (e.g., in an "About" or "Credits" section).
 */

package app.morphe.patches.music.utils.settings

import app.morphe.patcher.patch.ResourcePatchContext
import app.morphe.patches.music.utils.compatibility.Constants.YOUTUBE_MUSIC_PACKAGE_NAME
import app.morphe.patches.music.utils.patch.PatchList
import app.morphe.util.adoptChild
import app.morphe.util.cloneNodes
import app.morphe.util.doRecursively
import app.morphe.util.findElementByAttributeValueOrThrow
import app.morphe.util.insertNode
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

internal object ResourceUtils {
    private lateinit var context: ResourcePatchContext

    fun setContext(context: ResourcePatchContext) {
        this.context = context
    }

    private const val RVX_SETTINGS_KEY = "revanced_settings"
    private const val REVANCED_SETTINGS_INTENT = "revanced_settings_intent"

    const val SETTINGS_HEADER_PATH = "res/xml/settings_headers.xml"
    const val RVX_PREFERENCE_PATH = "res/xml/revanced_prefs.xml"

    const val PREFERENCE_SCREEN_TAG_NAME =
        "PreferenceScreen"

    const val PREFERENCE_CATEGORY_TAG_NAME =
        "com.google.android.apps.youtube.music.ui.preference.PreferenceCategoryCompat"

    const val SWITCH_PREFERENCE_TAG_NAME =
        "com.google.android.apps.youtube.music.ui.preference.SwitchCompatPreference"
    const val LIST_PREFERENCE_TAG_NAME =
        "app.morphe.extension.shared.settings.preference.CustomDialogListPreference"
    const val TEXT_PREFERENCE_TAG_NAME =
        "app.morphe.extension.shared.settings.preference.ResettableEditTextPreference"

    const val ACTIVITY_HOOK_TARGET_CLASS =
        "com.google.android.gms.common.api.GoogleApiActivity"

    var gmsCorePackageName = "app.revanced.android.gms"
    var musicPackageName = YOUTUBE_MUSIC_PACKAGE_NAME

    private var iconType = "default"
    fun getIconType() = iconType

    fun setIconType(iconName: String) {
        iconType = iconName
    }

    private fun isIncludedCategory(category: String): Boolean {
        CategoryType.entries.forEach { preference ->
            if (category == preference.value)
                return preference.added
        }
        return false
    }

    private fun replacePackageName() = context.apply {
        val xmlFile = get(SETTINGS_HEADER_PATH)
        xmlFile.writeText(
            xmlFile.readText()
                .replace(
                    "\"com.google.android.apps.youtube.music\"",
                    "\"" + musicPackageName + "\""
                )
        )
    }


    private fun setPreferenceCategory(newCategory: String) {
        CategoryType.entries.forEach { preference ->
            if (newCategory == preference.value)
                preference.added = true
        }
    }

    fun updatePackageName(
        newGmsCorePackage: String,
        newMusicPackage: String,
    ) {
        gmsCorePackageName = newGmsCorePackage
        musicPackageName = newMusicPackage
        replacePackageName()
    }

    fun updatePatchStatus(patch: PatchList) {
        patch.included = true
    }

    fun addPreferenceCategory(category: String) {
        context.document(SETTINGS_HEADER_PATH).use { document ->
            val tags = document.getElementsByTagName(PREFERENCE_SCREEN_TAG_NAME)
            List(tags.length) { tags.item(it) as Element }
                .filter { it.getAttribute("android:key").contains(RVX_SETTINGS_KEY) }
                .forEach {
                    if (!isIncludedCategory(category)) {
                        it.adoptChild(PREFERENCE_SCREEN_TAG_NAME) {
                            setAttribute(
                                "android:title",
                                "@string/revanced_preference_screen_$category" + "_title"
                            )
                            setAttribute("android:key", "revanced_preference_screen_$category")
                        }
                        setPreferenceCategory(category)
                    }
                }
        }
    }

    /**
     * Applies a DOM mutation to every generated RVX Music preference screen for a category.
     * This keeps nested custom screens in the same XML flow as the existing settings helpers.
     */
    fun editPreferenceCategory(category: String, action: Element.() -> Unit) {
        context.document(SETTINGS_HEADER_PATH).use { document ->
            val tags = document.getElementsByTagName(PREFERENCE_SCREEN_TAG_NAME)
            List(tags.length) { tags.item(it) as Element }
                .filter {
                    it.getAttribute("android:key").contains("revanced_preference_screen_$category")
                }
                .forEach { it.action() }
        }
    }

    fun addPreferenceCategoryUnderPreferenceScreen(
        preferenceScreenKey: String,
        category: String
    ) {
        context.document(SETTINGS_HEADER_PATH).use { document ->
            val tags = document.getElementsByTagName(PREFERENCE_SCREEN_TAG_NAME)
            List(tags.length) { tags.item(it) as Element }
                .filter { it.getAttribute("android:key").contains(preferenceScreenKey) }
                .forEach {
                    it.adoptChild(PREFERENCE_CATEGORY_TAG_NAME) {
                        setAttribute("android:title", "@string/$category")
                        setAttribute("android:key", category)
                    }
                }
        }
    }

    fun sortPreferenceCategory(
        category: String
    ) {
        context.document(SETTINGS_HEADER_PATH).use { document ->
            document.doRecursively node@{
                if (it !is Element) return@node

                it.getAttributeNode("android:key")?.let { attribute ->
                    if (attribute.textContent == "revanced_preference_screen_$category") {
                        it.cloneNodes(it.parentNode)
                    }
                }
            }
        }
        replacePackageName()
    }

    fun addGmsCorePreference(
        category: String,
        key: String,
        packageName: String,
        targetClassName: String
    ) {
        context.document(SETTINGS_HEADER_PATH).use { document ->
            val tags = document.getElementsByTagName(PREFERENCE_SCREEN_TAG_NAME)
            List(tags.length) { tags.item(it) as Element }
                .filter {
                    it.getAttribute("android:key").contains("revanced_preference_screen_$category")
                }
                .forEach {
                    it.adoptChild("Preference") {
                        setAttribute("android:key", key)
                        setAttribute("android:title", "@string/$key" + "_title")
                        setAttribute("android:summary", "@string/$key" + "_summary")
                        this.adoptChild("intent") {
                            setAttribute("android:targetPackage", packageName)
                            setAttribute("android:data", key)
                            setAttribute(
                                "android:targetClass",
                                targetClassName
                            )
                        }
                    }
                }
        }
    }

    fun addPreferenceFragment(
        key: String,
        insertKey: String,
        targetClass: String,
    ) = context.apply {
        document(SETTINGS_HEADER_PATH).use { document ->
            with(document) {
                val processedKeys = mutableSetOf<String>() // To track processed keys

                doRecursively loop@{ node ->
                    if (node !is Element) return@loop // Skip if not an element

                    val attributeNode = node.getAttributeNode("android:key")
                        ?: return@loop // Skip if no key attribute
                    val currentKey = attributeNode.textContent

                    // Check if the current key has already been processed
                    if (processedKeys.contains(currentKey)) {
                        return@loop // Skip if already processed
                    } else {
                        processedKeys.add(currentKey) // Add the current key to processedKeys
                    }

                    when (currentKey) {
                        insertKey -> {
                            node.insertNode("Preference", node) {
                                setAttribute("android:key", "${key}_key")
                                setAttribute("android:title", "@string/${key}_title")
                                this.appendChild(
                                    ownerDocument.createElement("intent").also { intentNode ->
                                        intentNode.setAttribute(
                                            "android:targetPackage",
                                            "com.google.android.apps.youtube.music"
                                        )
                                        intentNode.setAttribute("android:data", key + "_intent")
                                        intentNode.setAttribute("android:targetClass", targetClass)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Modify the manifest to enhance TargetActivity behavior:
        // 1. Add a data intent filter with MIME type "text/plain".
        //    Some devices crash if undeclared data is passed to an intent,
        //    and this change appears to fix the issue.
        // 2. Add android:configChanges="orientation|screenSize|keyboardHidden".
        //    This prevents the activity from being recreated on configuration changes
        //    (e.g., screen rotation), preserving its current state and fragment.
        document("AndroidManifest.xml").use { document ->
            val activityElement = document.childNodes.findElementByAttributeValueOrThrow(
                "android:name",
                targetClass,
            )

            if (!activityElement.hasAttribute("android:configChanges")) {
                activityElement.setAttribute(
                    "android:configChanges",
                    "keyboardHidden|orientation|screenSize"
                )
            }

            val mimeType = document.createElement("data")
            mimeType.setAttribute("android:mimeType", "text/plain")

            val intentFilter = document.createElement("intent-filter")
            intentFilter.appendChild(mimeType)

            activityElement.appendChild(intentFilter)
        }
    }

    fun addSwitchPreference(
        category: String,
        key: String,
        defaultValue: String,
        dependencyKey: String,
        setSummary: Boolean
    ) {
        context.document(SETTINGS_HEADER_PATH).use { document ->
            val tags = document.getElementsByTagName(PREFERENCE_SCREEN_TAG_NAME)
            List(tags.length) { tags.item(it) as Element }
                .filter {
                    it.getAttribute("android:key").contains("revanced_preference_screen_$category")
                }
                .forEach {
                    it.adoptChild(SWITCH_PREFERENCE_TAG_NAME) {
                        setAttribute("android:title", "@string/$key" + "_title")
                        if (setSummary) {
                            setAttribute("android:summary", "@string/$key" + "_summary")
                        }
                        setAttribute("android:key", key)
                        setAttribute("android:defaultValue", defaultValue)
                        if (dependencyKey != "") {
                            setAttribute("android:dependency", dependencyKey)
                        }
                    }
                }
        }
    }

    fun addPreferenceWithIntent(
        category: String,
        key: String,
        dependencyKey: String,
        setSummary: Boolean,
    ) {
        context.document(SETTINGS_HEADER_PATH).use { document ->
            val tags = document.getElementsByTagName(PREFERENCE_SCREEN_TAG_NAME)
            List(tags.length) { tags.item(it) as Element }
                .filter {
                    it.getAttribute("android:key").contains("revanced_preference_screen_$category")
                }
                .forEach {
                    it.adoptChild("Preference") {
                        setAttribute("android:title", "@string/$key" + "_title")
                        if (setSummary) {
                            setAttribute("android:summary", "@string/$key" + "_summary")
                        }
                        setAttribute("android:key", key)
                        if (dependencyKey.isNotEmpty()) {
                            setAttribute("android:dependency", dependencyKey)
                        }
                        this.adoptChild("intent") {
                            setAttribute("android:targetPackage", musicPackageName)
                            setAttribute("android:data", key)
                            setAttribute(
                                "android:targetClass",
                                ACTIVITY_HOOK_TARGET_CLASS
                            )
                        }
                    }
                }
        }
    }

    /**
     * Adds an in-process custom preference using its extension class as the XML tag.
     * This is required for preferences that own their click handling and dialog lifecycle.
     */
    fun addCustomPreference(
        category: String,
        key: String,
        tag: String,
        dependencyKey: String,
        setSummary: Boolean,
    ) {
        context.document(SETTINGS_HEADER_PATH).use { document ->
            val tags = document.getElementsByTagName(PREFERENCE_SCREEN_TAG_NAME)
            List(tags.length) { tags.item(it) as Element }
                .filter {
                    it.getAttribute("android:key").contains("revanced_preference_screen_$category")
                }
                .forEach {
                    it.adoptChild(tag) {
                        setAttribute("android:title", "@string/${key}_title")
                        if (setSummary) {
                            setAttribute("android:summary", "@string/${key}_summary")
                        }
                        setAttribute("android:key", key)
                        setAttribute("android:selectable", "true")
                        if (dependencyKey.isNotEmpty()) {
                            setAttribute("android:dependency", dependencyKey)
                        }
                    }
                }
        }
    }

    /**
     * Adds an in-process list preference. Unlike intent-backed preferences, this keeps the
     * dialog attached to the searchable settings activity on YouTube Music 9.15+.
     */
    fun addListPreference(
        category: String,
        key: String,
        dependencyKey: String,
        setSummary: Boolean,
    ) {
        context.document(SETTINGS_HEADER_PATH).use { document ->
            val tags = document.getElementsByTagName(PREFERENCE_SCREEN_TAG_NAME)
            List(tags.length) { tags.item(it) as Element }
                .filter {
                    it.getAttribute("android:key").contains("revanced_preference_screen_$category")
                }
                .forEach {
                    it.adoptChild(LIST_PREFERENCE_TAG_NAME) {
                        setAttribute("android:title", "@string/${key}_title")
                        if (setSummary) {
                            setAttribute("android:summary", "@string/${key}_summary")
                        }
                        setAttribute("android:key", key)
                        setAttribute("android:entries", "@array/${key}_entries")
                        setAttribute("android:entryValues", "@array/${key}_entry_values")
                        if (dependencyKey.isNotEmpty()) {
                            setAttribute("android:dependency", dependencyKey)
                        }
                    }
                }
        }
    }

    /** Adds an in-process text dialog preference using the shared custom dialog. */
    fun addTextPreference(
        category: String,
        key: String,
        dependencyKey: String,
        setSummary: Boolean,
    ) {
        context.document(SETTINGS_HEADER_PATH).use { document ->
            val tags = document.getElementsByTagName(PREFERENCE_SCREEN_TAG_NAME)
            List(tags.length) { tags.item(it) as Element }
                .filter {
                    it.getAttribute("android:key").contains("revanced_preference_screen_$category")
                }
                .forEach {
                    it.adoptChild(TEXT_PREFERENCE_TAG_NAME) {
                        setAttribute("android:title", "@string/${key}_title")
                        if (setSummary) {
                            setAttribute("android:summary", "@string/${key}_summary")
                        }
                        setAttribute("android:key", key)
                        setAttribute("android:inputType", "text")
                        if (dependencyKey.isNotEmpty()) {
                            setAttribute("android:dependency", dependencyKey)
                        }
                    }
                }
        }
    }

    fun addRVXSettingsPreference(insertKey: String) {
        context.document(SETTINGS_HEADER_PATH).use { document ->
            document.doRecursively node@{
                if (it !is Element) return@node

                it.getAttributeNode("android:key")?.let { attribute ->
                    if (attribute.textContent == insertKey && it.getAttributeNode(
                            "app:allowDividerBelow"
                        ).textContent == "false"
                    ) {
                        it.insertNode(PREFERENCE_SCREEN_TAG_NAME, it) {
                            setAttribute(
                                "android:title",
                                "@string/revanced_settings_title"
                            )
                            setAttribute("android:key", "revanced_settings")
                            setAttribute("app:allowDividerAbove", "false")
                            this.adoptChild("intent") {
                                setAttribute("android:targetPackage", musicPackageName)
                                setAttribute("android:data", REVANCED_SETTINGS_INTENT)
                                setAttribute(
                                    "android:targetClass",
                                    ACTIVITY_HOOK_TARGET_CLASS
                                )
                            }
                        }
                        it.getAttributeNode("app:allowDividerBelow").textContent = "true"
                        return@node
                    }
                }
            }

            document.doRecursively node@{
                if (it !is Element) return@node

                it.getAttributeNode("app:allowDividerBelow")?.let { attribute ->
                    if (attribute.textContent == "true") {
                        attribute.textContent = "false"
                    }
                }
            }
        }
    }

    fun writeSearchPreferenceFile() {
        context.document(SETTINGS_HEADER_PATH).use { document ->
            var rvxSettingsElement: Element? = null
            document.doRecursively node@{
                if (rvxSettingsElement != null || it !is Element) return@node

                if (it.getAttribute("android:key") == RVX_SETTINGS_KEY) {
                    rvxSettingsElement = it
                }
            }

            val sourceElement = rvxSettingsElement ?: return
            val searchDocument = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .newDocument()
            val searchRoot = searchDocument.importNode(sourceElement, true) as Element
            searchRoot.setAttribute("xmlns:android", "http://schemas.android.com/apk/res/android")
            searchRoot.setAttribute("xmlns:app", "http://schemas.android.com/apk/res-auto")
            searchDocument.appendChild(searchRoot)

            removeRootIntent(searchRoot)
            normalizeSearchPreferenceTags(searchRoot)

            TransformerFactory.newInstance()
                .newTransformer()
                .apply {
                    setOutputProperty(OutputKeys.INDENT, "yes")
                    setOutputProperty(OutputKeys.ENCODING, "utf-8")
                }
                .transform(
                    DOMSource(searchDocument),
                    StreamResult(context.get(RVX_PREFERENCE_PATH))
                )

            // Clear child elements of sourceElement in settings_headers.xml to prevent AndroidX preference inflation crash.
            val childrenList = List(sourceElement.childNodes.length) { sourceElement.childNodes.item(it) }
            for (child in childrenList) {
                if (child is Element && child.tagName != "intent") {
                    sourceElement.removeChild(child)
                }
            }
        }
    }

    private fun removeRootIntent(root: Element) {
        val children = root.childNodes
        for (i in children.length - 1 downTo 0) {
            val child = children.item(i) as? Element ?: continue
            if (child.tagName == "intent") {
                root.removeChild(child)
            }
        }
    }

    private fun normalizeSearchPreferenceTags(element: Element) {
        val normalizedElement = when (element.tagName) {
            PREFERENCE_CATEGORY_TAG_NAME ->
                element.ownerDocument.renameNode(element, null, "PreferenceCategory") as Element

            SWITCH_PREFERENCE_TAG_NAME ->
                element.ownerDocument.renameNode(element, null, "SwitchPreference") as Element

            else -> element
        }

        normalizedElement.removeAttribute("app:allowDividerAbove")
        normalizedElement.removeAttribute("app:allowDividerBelow")

        val children = normalizedElement.childNodes
        for (i in 0 until children.length) {
            val child = children.item(i) as? Element ?: continue
            normalizeSearchPreferenceTags(child)
        }
    }

    fun addLinkPreference(
        category: String,
        key: String,
        url: String
    ) {
        context.document(SETTINGS_HEADER_PATH).use { document ->
            val tags = document.getElementsByTagName(PREFERENCE_SCREEN_TAG_NAME)
            List(tags.length) { tags.item(it) as Element }
                .filter {
                    it.getAttribute("android:key").contains("revanced_preference_screen_$category")
                }
                .forEach {
                    it.adoptChild("Preference") {
                        setAttribute("android:title", "@string/$key" + "_title")
                        setAttribute("android:key", key)

                        this.adoptChild("intent") {
                            setAttribute("android:action", "android.intent.action.VIEW")
                            setAttribute("android:data", url)
                        }
                    }
                }
        }
    }
}
