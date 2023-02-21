package app.revanced.util.resources

import app.revanced.extensions.doRecursively
import app.revanced.patcher.data.ResourceContext
import org.w3c.dom.Element
import org.w3c.dom.Node

private fun Node.adoptChild(tagName: String, block: Element.() -> Unit) {
    val child = ownerDocument.createElement(tagName)
    child.block()
    appendChild(child)
}

private fun Node.cloneNodes(parent: Node) {
    val node = cloneNode(true)
    parent.appendChild(node)
    parent.removeChild(this)
}

private fun Node.insertNode(tagName: String, targetNode: Node, block: Element.() -> Unit) {
    val child = ownerDocument.createElement(tagName)
    child.block()
    parentNode.insertBefore(child, targetNode)
}


internal object MusicResourceHelper {

    private const val YOUTUBE_MUSIC_SETTINGS_PATH = "res/xml/settings_headers.xml"

    private const val YOUTUBE_MUSIC_SETTINGS_KEY = "revanced_extended_settings"

    private const val YOUTUBE_MUSIC_CATEGORY_TAG_NAME = "com.google.android.apps.youtube.music.ui.preference.PreferenceCategoryCompat"

    private const val YOUTUBE_MUSIC_PREFERENCE_TAG_NAME = "com.google.android.apps.youtube.music.ui.preference.SwitchCompatPreference"

    private var currentMusicPreferenceCategory = emptyArray<String>()

    private fun setMusicPreferenceCategory (newCategory: String) {
        currentMusicPreferenceCategory += listOf(newCategory)
    }

    internal fun ResourceContext.addMusicPreferenceCategory(
        category: String
    ) {
        this.xmlEditor[YOUTUBE_MUSIC_SETTINGS_PATH].use { editor ->
            val tags = editor.file.getElementsByTagName("PreferenceScreen")
            List(tags.length) { tags.item(it) as Element }
                .filter { it.getAttribute("android:key").contains(YOUTUBE_MUSIC_SETTINGS_KEY) }
                .forEach {
                    if (!currentMusicPreferenceCategory.contains(category)) {
                        it.adoptChild(YOUTUBE_MUSIC_CATEGORY_TAG_NAME) {
                            setAttribute("android:title", "@string/revanced_category_$category")
                            setAttribute("android:key", "revanced_settings_$category")
                        }
                        setMusicPreferenceCategory(category)
                    }
                }
        }
    }

    internal fun ResourceContext.sortMusicPreferenceCategory() {
        this.xmlEditor[YOUTUBE_MUSIC_SETTINGS_PATH].use { editor ->
            editor.file.doRecursively loop@{
                if (it !is Element) return@loop

                currentMusicPreferenceCategory.forEach { category ->
                    it.getAttributeNode("android:key")?.let { attribute ->
                        if (attribute.textContent == "revanced_settings_$category") {
                            it.cloneNodes(it.parentNode)
                        }
                    }
                }
            }
        }
    }

    internal fun ResourceContext.addMusicPreference(
        category: String,
        key: String,
        defaultValue: String
    ) {
        this.xmlEditor[YOUTUBE_MUSIC_SETTINGS_PATH].use { editor ->
            val tags = editor.file.getElementsByTagName(YOUTUBE_MUSIC_CATEGORY_TAG_NAME)
            List(tags.length) { tags.item(it) as Element }
                .filter { it.getAttribute("android:key").contains("revanced_settings_$category") }
                .forEach {
                    it.adoptChild(YOUTUBE_MUSIC_PREFERENCE_TAG_NAME) {
                        setAttribute("android:title", "@string/$key" + "_title")
                        setAttribute("android:key", key)
                        setAttribute("android:defaultValue", defaultValue)
                    }
                }
        }
    }

    internal fun ResourceContext.addReVancedMusicPreference() {
        this.xmlEditor[YOUTUBE_MUSIC_SETTINGS_PATH].use { editor ->
            with (editor.file) {
                doRecursively loop@{
                    if (it !is Element) return@loop
                    it.getAttributeNode("android:key")?.let { attribute ->
                        if (attribute.textContent == "settings_header_about_youtube_music" && it.getAttributeNode("app:allowDividerBelow").textContent == "false") {
                            it.insertNode("PreferenceScreen", it) {
                                setAttribute("android:title", "@string/" + YOUTUBE_MUSIC_SETTINGS_KEY + "_title")
                                setAttribute("android:summary", "@string/" + YOUTUBE_MUSIC_SETTINGS_KEY + "_summary")
                                setAttribute("android:key", YOUTUBE_MUSIC_SETTINGS_KEY)
                            }
                            it.getAttributeNode("app:allowDividerBelow").textContent = "true"
                            return@loop
                        }
                    }
                }

                doRecursively loop@{
                    if (it !is Element) return@loop

                    it.getAttributeNode("app:allowDividerBelow")?.let { attribute ->
                        if (attribute.textContent == "true") {
                            attribute.textContent = "false"
                        }
                    }
                }
            }
        }
    }

}