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
    name = "Tuck away preferences",
    description = "Force to hide settings menu elements. Prefs \"Account\" and \"Your data in YouTube\" will be ignored if you add them as they may cause a crash.",
    compatiblePackages = [CompatiblePackage("com.google.android.youtube")]
)
@Suppress("unused")
object HideSettingsPrefsPatch : ResourcePatch() {

    private const val DEFAULT_ELEMENTS = "Data saving, Video quality preferences, Manage all history, Privacy, Try experimental new features, " +
        "Purchases and memberships, Billing & payments, Connected apps, Live chat, Captions, About"

    private val SettingElements by stringPatchOption(
        key = "SettingElements",
        default = DEFAULT_ELEMENTS,
        title = "Main settings elements",
        description = "Hide main settings elements."
    )

    private val DEFAULT_ELEMENTS_MAP = mapOf(
        "parent settings" to "parent_tools_key",
        "general" to "general_key",
        // "account" to "account_switcher_key",
        "data saving" to "data_saving_settings_key",
        "autoplay" to "auto_play_key",
        "video quality preferences" to "video_quality_settings_key",
        "background" to "offline_key",
        "watch on tv" to "pair_with_tv_key",
        "manage all history" to "history_key",
        // "your data in youtube" to "your_data_key",
        "privacy" to "privacy_key",
        "history & privacy" to "privacy_key",
        "try experimental new features" to "premium_early_access_browse_page_key",
        "purchases and memberships" to "subscription_product_setting_key",
        "billing & payments" to "billing_and_payment_key",
        "billing and payments" to "billing_and_payment_key",
        "notifications" to "notification_key",
        "connected apps" to "connected_accounts_browse_page_key",
        "live chat" to "live_chat_key",
        "captions" to "captions_key",
        "accessibility" to "accessibility_settings_key",
        "about" to "about_key"
    )

    // Function to parse comma-separated string into a list of strings
    private fun parseElementsFromString(elementsString: String?): List<String> {
        return elementsString?.split(",")!!.map { it.trim() }
    }

    override fun execute(context: ResourceContext) {

        val elementsToHide = mutableListOf<String>()

        parseElementsFromString(SettingElements).forEach { element ->
            val key = DEFAULT_ELEMENTS_MAP[element.lowercase()]
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

        SettingsPatch.updatePatchStatus("Tuck away preferences")
    }
}
