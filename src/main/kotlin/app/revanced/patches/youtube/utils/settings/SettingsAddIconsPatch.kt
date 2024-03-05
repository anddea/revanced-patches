package app.revanced.patches.youtube.utils.settings

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.util.ResourceGroup
import app.revanced.util.copyResources
import org.w3c.dom.Element

@Patch(
    name = "Settings icons",
    description = "Adds icons to specific preferences in the settings.",
    dependencies = [SettingsPatch::class],
    compatiblePackages = [CompatiblePackage("com.google.android.youtube", [])], // Adjust package name and version code
    use = true
)
@Suppress("unused")
object SettingsAddIconsPatch : ResourcePatch() {
    override fun execute(context: ResourceContext) {

        val validTitles = setOf(
            "revanced_overlay_button_always_repeat",
            "revanced_overlay_button_copy_video_url_timestamp",
            "revanced_overlay_button_copy_video_url",
            "revanced_overlay_button_speed_dialog",
            "revanced_overlay_button_time_ordered_playlist",
            "revanced_overlay_button_whitelisting",
            "revanced_overlay_button_external_downloader"
        )

        arrayOf(
            ResourceGroup(
                "drawable-xxhdpi",
                *validTitles.map { it + "_icon.png" }.toTypedArray(),
                "empty_icon.png"
            )
        ).forEach { resourceGroup ->
            context.copyResources("youtube/settings", resourceGroup)
        }

        context.xmlEditor["res/xml/revanced_prefs.xml"].use { editor ->
            val switchPreferences = editor.file.getElementsByTagName("SwitchPreference")
            for (i in 0 until switchPreferences.length) {
                val preference = switchPreferences.item(i) as? Element
                val title = preference?.getAttribute("android:key")
                if (title in validTitles) {
                    val drawableName = title + "_icon"
                    preference?.setAttribute("android:icon", "@drawable/$drawableName")
                }
            }

            val preferenceScreens = editor.file.getElementsByTagName("PreferenceScreen")
            for (i in 0 until preferenceScreens.length) {
                val preference = preferenceScreens.item(i) as? Element
                val title = preference?.getAttribute("android:key")
                if (title == "whitelisting" || title == "external_downloader") {
                    preference.setAttribute("android:icon", "@drawable/empty_icon")
                }
            }
        }

        SettingsPatch.updatePatchStatus("Settings icons")

    }
}
