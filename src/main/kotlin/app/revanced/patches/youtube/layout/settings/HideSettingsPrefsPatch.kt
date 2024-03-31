package app.revanced.patches.youtube.layout.settings

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.stringPatchOption
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import org.w3c.dom.Element

@Patch(
    name = "Hide settings preferences",
    description = "Force to hide settings menu elements. Prefs \"About\" (it won't add ReVanced/SB/RYD settings) and \"Account\" (it will crash the app) will be ignored if you add them.",
    compatiblePackages = [CompatiblePackage("com.google.android.youtube")]
)
@Suppress("unused")
object HideSettingsPrefsPatch : ResourcePatch() {

    private const val DEFAULT_ELEMENTS = "General, Data saving, Autoplay, " +
        "Video quality preferences, Background, Watch on TV, History & privacy, " +
        "Try experimental new features, Notifications, Captions, Accessibility"

    private val SettingElements by stringPatchOption(
        key = "SettingElements",
        default = DEFAULT_ELEMENTS,
        title = "Main settings elements",
        description = "Hide main settings elements."
    )

    private val DEFAULT_ELEMENTS_MAP = mapOf(
        "General" to "general_key",
        "Account" to "account_switcher_key",
        "Data saving" to "data_saving_settings_key",
        "Autoplay" to "auto_play_key",
        "Video quality preferences" to "video_quality_settings_key",
        "Background" to "offline_key", // no
        "Watch on TV" to "pair_with_tv_key",
        "History & privacy" to "privacy_key",
        "Try experimental new features" to "premium_early_access_browse_page_key",
        "Notifications" to "notification_key",
        "Captions" to "captions_key",
        "Accessibility" to "accessibility_settings_key"
    )

    // Function to parse comma-separated string into a list of strings
    private fun parseElementsFromString(elementsString: String?): List<String> {
        return elementsString?.split(",")!!.map { it.trim() }
    }

    override fun execute(context: ResourceContext) {

        val elementsToHide = mutableListOf<String>()

        parseElementsFromString(SettingElements).forEach { element ->
            val key = DEFAULT_ELEMENTS_MAP[element]
            if (key != null) {
                elementsToHide.add("@string/$key")
            } else {
                println("WARNING: Unknown element '$element' will be skipped")
            }
        }

        context.xmlEditor["res/xml/settings_fragment.xml"].use { editor ->
            val preferenceElements = editor.file.getElementsByTagName("Preference")

            for (i in preferenceElements.length - 1 downTo 0) {
                val item = preferenceElements.item(i) as? Element
                val titleAttribute = item?.getAttribute("android:key")
                if (titleAttribute in elementsToHide) {
                    item?.parentNode?.removeChild(item)
                }
            }
        }

        SettingsPatch.updatePatchStatus("Hide settings preferences")
    }
}
