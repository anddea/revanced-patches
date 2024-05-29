package app.revanced.patches.music.utils.settings

import app.revanced.patcher.data.ResourceContext
import app.revanced.util.adoptChild
import app.revanced.util.cloneNodes
import app.revanced.util.doRecursively
import app.revanced.util.insertNode
import org.w3c.dom.Element

@Suppress("DEPRECATION")
object ResourceUtils {

    private const val RVX_SETTINGS_KEY = "revanced_extended_settings"

    const val SETTINGS_HEADER_PATH = "res/xml/settings_headers.xml"

    const val PREFERENCE_SCREEN_TAG_NAME =
        "PreferenceScreen"

    const val PREFERENCE_CATEGORY_TAG_NAME =
        "com.google.android.apps.youtube.music.ui.preference.PreferenceCategoryCompat"

    const val SWITCH_PREFERENCE_TAG_NAME =
        "com.google.android.apps.youtube.music.ui.preference.SwitchCompatPreference"

    const val ACTIVITY_HOOK_TARGET_CLASS =
        "com.google.android.gms.common.api.GoogleApiActivity"

    var musicPackageName = "com.google.android.apps.youtube.music"

    private fun isIncludedCategory(category: String): Boolean {
        CategoryType.entries.forEach { preference ->
            if (category == preference.value)
                return preference.added
        }
        return false
    }

    private fun ResourceContext.replacePackageName() {
        this[SETTINGS_HEADER_PATH].writeText(
            this[SETTINGS_HEADER_PATH].readText()
                .replace("\"com.google.android.apps.youtube.music\"", "\"" + musicPackageName + "\"")
        )
    }

    private fun setPreferenceCategory(newCategory: String) {
        CategoryType.entries.forEach { preference ->
            if (newCategory == preference.value)
                preference.added = true
        }
    }

    fun ResourceContext.updatePackageName(newPackage: String) {
        musicPackageName = newPackage
        replacePackageName()
    }

    fun ResourceContext.addPreferenceCategory(
        category: String
    ) {
        this.xmlEditor[SETTINGS_HEADER_PATH].use { editor ->
            val tags = editor.file.getElementsByTagName(PREFERENCE_SCREEN_TAG_NAME)
            List(tags.length) { tags.item(it) as Element }
                .filter { it.getAttribute("android:key").contains(RVX_SETTINGS_KEY) }
                .forEach {
                    if (!isIncludedCategory(category)) {
                        it.adoptChild(PREFERENCE_SCREEN_TAG_NAME) {
                            setAttribute("android:title", "@string/revanced_preference_screen_$category" + "_title")
                            setAttribute("android:key", "revanced_preference_screen_$category")
                        }
                        setPreferenceCategory(category)
                    }
                }
        }
    }

    fun ResourceContext.addPreferenceCategoryUnderPreferenceScreen(
        preferenceScreenKey: String,
        category: String
    ) {
        this.xmlEditor[SETTINGS_HEADER_PATH].use { editor ->
            val tags = editor.file.getElementsByTagName(PREFERENCE_SCREEN_TAG_NAME)
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

    fun ResourceContext.sortPreferenceCategory(
        category: String
    ) {
        this.xmlEditor[SETTINGS_HEADER_PATH].use { editor ->
            editor.file.doRecursively loop@{
                if (it !is Element) return@loop

                it.getAttributeNode("android:key")?.let { attribute ->
                    if (attribute.textContent == "revanced_preference_screen_$category") {
                        it.cloneNodes(it.parentNode)
                    }
                }
            }
        }
        replacePackageName()
    }

    fun ResourceContext.addMicroGPreference(
        category: String,
        key: String,
        packageName: String,
        targetClassName: String
    ) {
        this.xmlEditor[SETTINGS_HEADER_PATH].use { editor ->
            val tags = editor.file.getElementsByTagName(PREFERENCE_SCREEN_TAG_NAME)
            List(tags.length) { tags.item(it) as Element }
                .filter { it.getAttribute("android:key").contains("revanced_preference_screen_$category") }
                .forEach {
                    it.adoptChild("Preference") {
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

    fun ResourceContext.addSwitchPreference(
        category: String,
        key: String,
        defaultValue: String,
        dependencyKey: String,
        setSummary: Boolean
    ) {
        this.xmlEditor[SETTINGS_HEADER_PATH].use { editor ->
            val tags = editor.file.getElementsByTagName(PREFERENCE_SCREEN_TAG_NAME)
            List(tags.length) { tags.item(it) as Element }
                .filter { it.getAttribute("android:key").contains("revanced_preference_screen_$category") }
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

    fun ResourceContext.addPreferenceWithIntent(
        category: String,
        key: String,
        dependencyKey: String
    ) {
        this.xmlEditor[SETTINGS_HEADER_PATH].use { editor ->
            val tags = editor.file.getElementsByTagName(PREFERENCE_SCREEN_TAG_NAME)
            List(tags.length) { tags.item(it) as Element }
                .filter { it.getAttribute("android:key").contains("revanced_preference_screen_$category") }
                .forEach {
                    it.adoptChild("Preference") {
                        setAttribute("android:title", "@string/$key" + "_title")
                        setAttribute("android:summary", "@string/$key" + "_summary")
                        setAttribute("android:key", key)
                        if (dependencyKey != "") {
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

    fun ResourceContext.addRVXSettingsPreference() {
        this.xmlEditor[SETTINGS_HEADER_PATH].use { editor ->
            with(editor.file) {
                doRecursively loop@{
                    if (it !is Element) return@loop
                    it.getAttributeNode("android:key")?.let { attribute ->
                        if (attribute.textContent == "settings_header_about_youtube_music" && it.getAttributeNode(
                                "app:allowDividerBelow"
                            ).textContent == "false"
                        ) {
                            it.insertNode(PREFERENCE_SCREEN_TAG_NAME, it) {
                                setAttribute(
                                    "android:title",
                                    "@string/revanced_extended_settings_title"
                                )
                                setAttribute("android:key", "revanced_extended_settings")
                                setAttribute("app:allowDividerAbove", "false")
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