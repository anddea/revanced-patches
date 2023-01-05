package app.revanced.shared.util.resources

import app.revanced.patcher.data.ResourceContext
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import org.w3c.dom.Element

internal object ResourceHelper {

    fun addEntryValues(
        context: ResourceContext,
        speed: String
    ) {
        val path = "res/values/arrays.xml"

        context.xmlEditor[path].use { editor ->
            with(editor.file) {
                val resourcesNode = getElementsByTagName("resources").item(0) as Element

                val newElement: Element = createElement("item")

                for (i in 0 until resourcesNode.childNodes.length) {
                    val node = resourcesNode.childNodes.item(i) as? Element ?: continue

                    if (node.getAttribute("name") == "revanced_video_speed_entry_values") {
                        newElement.appendChild(createTextNode(speed))

                        node.appendChild(newElement)
                    }
                }
            }
        }
    }

    fun addEntries(
        context: ResourceContext,
        speed: String
    ) {
        val path = "res/values/arrays.xml"

        context.xmlEditor[path].use { editor ->
            with(editor.file) {
                val resourcesNode = getElementsByTagName("resources").item(0) as Element

                val newElement: Element = createElement("item")

                for (i in 0 until resourcesNode.childNodes.length) {
                    val node = resourcesNode.childNodes.item(i) as? Element ?: continue

                    if (node.getAttribute("name") == "revanced_video_speed_entries") {
                        newElement.appendChild(createTextNode(speed))

                        node.appendChild(newElement)
                    }
                }
            }
        }
        context[path].writeText(
            context[path].readText().replace("1.0x", "@string/shorts_speed_control_normal_label")
        )
    }

    fun addSpeed(
        context: ResourceContext
    ) {
        val prefs = context["res/xml/revanced_prefs.xml"]
        prefs.writeText(
            prefs.readText()
            .replace(
                "revanced_default_video_speed\"",
                "revanced_default_video_speed\" android:entries=\"@array/revanced_video_speed_entries\" android:entryValues=\"@array/revanced_video_speed_entry_values\""
            )
        )
    }

    fun addSettings(
        context: ResourceContext,
        PreferenceCategory: String,
        Preference: String,
        Settings: String
    ) {
        val prefs = context["res/xml/revanced_prefs.xml"]
        prefs.writeText(
            prefs.readText()
            .replace("<!-- $PreferenceCategory", "")
            .replace("$PreferenceCategory -->", "")
            .replace("<!-- $Preference", "")
            .replace("$Preference -->", "")
            .replace("<!-- $Settings", "")
            .replace("$Settings -->", "")
        )
    }

    fun addSettings2(
        context: ResourceContext,
        PreferenceCategory: String,
        Preference: String,
        Settings: String,
        Settings2: String
    ) {
        val prefs = context["res/xml/revanced_prefs.xml"]
        prefs.writeText(
            prefs.readText()
            .replace("<!-- $PreferenceCategory", "")
            .replace("$PreferenceCategory -->", "")
            .replace("<!-- $Preference", "")
            .replace("$Preference -->", "")
            .replace("<!-- $Settings", "")
            .replace("$Settings -->", "")
            .replace("<!-- $Settings2", "")
            .replace("$Settings2 -->", "")
        )
    }

    fun addSettings3(
        context: ResourceContext,
        PreferenceCategory: String,
        Preference: String,
        Settings: String,
        Settings2: String,
        Settings3: String
    ) {
        val prefs = context["res/xml/revanced_prefs.xml"]
        prefs.writeText(
            prefs.readText()
            .replace("<!-- $PreferenceCategory", "")
            .replace("$PreferenceCategory -->", "")
            .replace("<!-- $Preference", "")
            .replace("$Preference -->", "")
            .replace("<!-- $Settings", "")
            .replace("$Settings -->", "")
            .replace("<!-- $Settings2", "")
            .replace("$Settings2 -->", "")
            .replace("<!-- $Settings3", "")
            .replace("$Settings3 -->", "")
        )
    }

    fun addSettings4(
        context: ResourceContext,
        PreferenceCategory: String,
        Preference: String,
        Settings: String,
        Settings2: String,
        Settings3: String,
        Settings4: String
    ) {
        val prefs = context["res/xml/revanced_prefs.xml"]
        prefs.writeText(
            prefs.readText()
            .replace("<!-- $PreferenceCategory", "")
            .replace("$PreferenceCategory -->", "")
            .replace("<!-- $Preference", "")
            .replace("$Preference -->", "")
            .replace("<!-- $Settings", "")
            .replace("$Settings -->", "")
            .replace("<!-- $Settings2", "")
            .replace("$Settings2 -->", "")
            .replace("<!-- $Settings3", "")
            .replace("$Settings3 -->", "")
            .replace("<!-- $Settings4", "")
            .replace("$Settings4 -->", "")
        )
    }

    fun addSettings5(
        context: ResourceContext,
        PreferenceCategory: String,
        Preference: String,
        Settings: String,
        Settings2: String,
        Settings3: String,
        Settings4: String,
        Settings5: String
    ) {
        val prefs = context["res/xml/revanced_prefs.xml"]
        prefs.writeText(
            prefs.readText()
            .replace("<!-- $PreferenceCategory", "")
            .replace("$PreferenceCategory -->", "")
            .replace("<!-- $Preference", "")
            .replace("$Preference -->", "")
            .replace("<!-- $Settings", "")
            .replace("$Settings -->", "")
            .replace("<!-- $Settings2", "")
            .replace("$Settings2 -->", "")
            .replace("<!-- $Settings3", "")
            .replace("$Settings3 -->", "")
            .replace("<!-- $Settings4", "")
            .replace("$Settings4 -->", "")
            .replace("<!-- $Settings5", "")
            .replace("$Settings5 -->", "")
        )
    }

    fun initReVancedSettings(
        context: ResourceContext
    ) {
        val fragment = context["res/xml/settings_fragment.xml"]
        fragment.writeText(
            fragment.readText().replace(
                "DeveloperPrefsFragment\" app:iconSpaceReserved=\"false\" />",
                "DeveloperPrefsFragment\" app:iconSpaceReserved=\"false\" /><Preference android:title=\"@string/revanced_settings\" android:summary=\"@string/revanced_extended_settings\"><intent android:targetPackage=\"com.google.android.youtube\" android:data=\"revanced_settings\" android:targetClass=\"com.google.android.apps.youtube.app.settings.videoquality.VideoQualitySettingsActivity\"/></Preference><!-- PREFERENCE: RETURN_YOUTUBE_DISLIKE<Preference android:title=\"@string/revanced_ryd_settings_title\" android:summary=\"@string/revanced_ryd_settings_summary\"><intent android:targetPackage=\"com.google.android.youtube\" android:data=\"ryd_settings\" android:targetClass=\"com.google.android.apps.youtube.app.settings.videoquality.VideoQualitySettingsActivity\" /></Preference>PREFERENCE: RETURN_YOUTUBE_DISLIKE --><!-- PREFERENCE: SPONSOR_BLOCK<Preference android:title=\"@string/sb_settings\" android:summary=\"@string/sb_summary\"><intent android:targetPackage=\"com.google.android.youtube\" android:data=\"sponsorblock_settings\" android:targetClass=\"com.google.android.apps.youtube.app.settings.videoquality.VideoQualitySettingsActivity\" /></Preference>PREFERENCE: SPONSOR_BLOCK -->"
            )
        )
    }

    fun addTranslations(
        context: ResourceContext,
        sourceDirectory: String,
        languageArray: Array<String>
    ) {
        languageArray.forEach { language ->
            val directory = "values-$language-v21"
            val relativePath = "$language/strings.xml"

            context["res/$directory"].mkdir()

            Files.copy(
                this.javaClass.classLoader.getResourceAsStream("$sourceDirectory/translations/$relativePath")!!,
                context["res"].resolve("$directory/strings.xml").toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }

    fun addReVancedSettings(
        context: ResourceContext,
        Preference: String
    ) {
        val fragment = context["res/xml/settings_fragment.xml"]
        fragment.writeText(
            fragment.readText()
            .replace("<!-- $Preference", "")
            .replace("$Preference -->", "")
        )
    }

    fun patchSuccess(
        context: ResourceContext,
        name: String
    ) {
        val prefs = context["res/xml/revanced_prefs.xml"]
        prefs.writeText(
            prefs.readText().replace(
                "\"$name\" android:summary=\"@string/revanced_patches_excluded",
                "\"$name\" android:summary=\"@string/revanced_patches_included"
            )
        )
    }

    fun themePatchSuccess(
        context: ResourceContext,
        before: String,
        after: String
    ) {
        val prefs = context["res/xml/revanced_prefs.xml"]
        prefs.writeText(
            prefs.readText().replace(
                "@string/revanced_themes_$before",
                "@string/revanced_themes_$after"
            )
        )
    }

    fun iconPatchSuccess(
        context: ResourceContext,
        targeticon: String
    ) {
        val prefs = context["res/xml/revanced_prefs.xml"]
        prefs.writeText(
            prefs.readText()
            .replace(
                "@string/revanced_icons_blue",
                "@string/revanced_icons_default"
            ).replace(
                "@string/revanced_icons_red",
                "@string/revanced_icons_default"
            ).replace(
                "@string/revanced_icons_revancify",
                "@string/revanced_icons_default"
            )
            .replace(
                "@string/revanced_icons_default",
                "@string/revanced_icons_$targeticon"
            )
        )
    }

    fun labelPatchSuccess(
        context: ResourceContext,
        appName: String
    ) {
        val prefs = context["res/xml/revanced_prefs.xml"]
        prefs.writeText(
            prefs.readText().replace(
                "@string/revanced_labels_default",
                appName
            )
        )
    }

}