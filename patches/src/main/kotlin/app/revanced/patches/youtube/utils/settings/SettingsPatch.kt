package app.revanced.patches.youtube.utils.settings

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.patch.booleanOption
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patcher.patch.stringOption
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.shared.extension.Constants.EXTENSION_UTILS_CLASS_DESCRIPTOR
import app.revanced.patches.shared.extension.Constants.EXTENSION_UTILS_PATH
import app.revanced.patches.shared.mainactivity.injectConstructorMethodCall
import app.revanced.patches.shared.mainactivity.injectOnCreateMethodCall
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.extension.Constants.UTILS_PATH
import app.revanced.patches.youtube.utils.extension.sharedExtensionPatch
import app.revanced.patches.youtube.utils.fix.cairo.cairoSettingsPatch
import app.revanced.patches.youtube.utils.fix.playbackspeed.playbackSpeedWhilePlayingPatch
import app.revanced.patches.youtube.utils.fix.splash.darkModeSplashScreenPatch
import app.revanced.patches.youtube.utils.mainactivity.mainActivityResolvePatch
import app.revanced.patches.youtube.utils.patch.PatchList.SETTINGS_FOR_YOUTUBE
import app.revanced.patches.youtube.utils.playservice.versionCheckPatch
import app.revanced.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.revanced.util.ResourceGroup
import app.revanced.util.copyResources
import app.revanced.util.copyXmlNode
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.removeStringsElements
import app.revanced.util.valueOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import org.w3c.dom.Element
import java.nio.file.Files
import java.util.jar.Manifest

private const val EXTENSION_INITIALIZATION_CLASS_DESCRIPTOR =
    "$UTILS_PATH/InitializationPatch;"

private const val EXTENSION_THEME_METHOD_DESCRIPTOR =
    "$EXTENSION_UTILS_PATH/BaseThemeUtils;->setTheme(Ljava/lang/Enum;)V"

private val settingsBytecodePatch = bytecodePatch(
    description = "settingsBytecodePatch"
) {
    dependsOn(
        sharedExtensionPatch,
        sharedResourceIdPatch,
        mainActivityResolvePatch,
        versionCheckPatch,
    )

    execute {
        fun MutableMethod.injectCall(index: Int) {
            val register = getInstruction<OneRegisterInstruction>(index).registerA

            addInstructions(
                index + 1, """
                invoke-static {v$register}, $EXTENSION_THEME_METHOD_DESCRIPTOR
                return-object v$register
                """
            )
            removeInstruction(index)
        }

        // apply the current theme of the settings page
        themeSetterSystemFingerprint.matchOrThrow().let {
            it.method.apply {
                injectCall(implementation!!.instructions.size - 1)
                injectCall(it.patternMatch!!.startIndex)
            }
        }

        injectOnCreateMethodCall(
            EXTENSION_INITIALIZATION_CLASS_DESCRIPTOR,
            "setExtendedUtils"
        )
        injectOnCreateMethodCall(
            EXTENSION_INITIALIZATION_CLASS_DESCRIPTOR,
            "onCreate"
        )
        injectConstructorMethodCall(
            EXTENSION_UTILS_CLASS_DESCRIPTOR,
            "setActivity"
        )
    }
}

private const val DEFAULT_ELEMENT = "@string/about_key"
private const val DEFAULT_LABEL = "ReVanced Extended"

private val SETTINGS_ELEMENTS_MAP = mapOf(
    "Parent settings" to "@string/parent_tools_key",
    "General" to "@string/general_key",
    "Account" to "@string/account_switcher_key",
    "Data saving" to "@string/data_saving_settings_key",
    "Autoplay" to "@string/auto_play_key",
    "Video quality preferences" to "@string/video_quality_settings_key",
    "Background" to "@string/offline_key",
    "Watch on TV" to "@string/pair_with_tv_key",
    "Manage all history" to "@string/history_key",
    "Your data in YouTube" to "@string/your_data_key",
    "Privacy" to "@string/privacy_key",
    "History & privacy" to "@string/privacy_key",
    "Try experimental new features" to "@string/premium_early_access_browse_page_key",
    "Purchases and memberships" to "@string/subscription_product_setting_key",
    "Billing & payments" to "@string/billing_and_payment_key",
    "Billing and payments" to "@string/billing_and_payment_key",
    "Notifications" to "@string/notification_key",
    "Connected apps" to "@string/connected_accounts_browse_page_key",
    "Live chat" to "@string/live_chat_key",
    "Captions" to "@string/captions_key",
    "Accessibility" to "@string/accessibility_settings_key",
    "About" to DEFAULT_ELEMENT
)

private lateinit var customName: String

val settingsPatch = resourcePatch(
    SETTINGS_FOR_YOUTUBE.title,
    SETTINGS_FOR_YOUTUBE.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsBytecodePatch,
        cairoSettingsPatch,
        darkModeSplashScreenPatch,
        playbackSpeedWhilePlayingPatch,
    )

    val insertPosition = stringOption(
        key = "insertPosition",
        default = DEFAULT_ELEMENT,
        values = SETTINGS_ELEMENTS_MAP,
        title = "Insert position",
        description = "The settings menu name that the RVX settings menu should be above.",
        required = true,
    )

    val settingsLabel = stringOption(
        key = "settingsLabel",
        default = DEFAULT_LABEL,
        title = "RVX settings label",
        description = "The name of the RVX settings menu.",
        required = true,
    )

    val settingsSummaries by booleanOption(
        key = "settingsSummaries",
        default = true,
        title = "RVX settings summaries",
        description = "Shows the summary / description of each RVX setting. If set to false, no descriptions will be provided.",
        required = true,
    )

    execute {
        /**
         * check patch options
         */
        customName = settingsLabel
            .valueOrThrow()

        val insertKey = insertPosition
            .valueOrThrow()

        ResourceUtils.setContext(this)

        /**
         * remove strings duplicated with RVX resources
         *
         * YouTube does not provide translations for these strings.
         * That's why it's been added to RVX resources.
         * This string also exists in RVX resources, so it must be removed to avoid being duplicated.
         */
        removeStringsElements(
            arrayOf("values"),
            arrayOf(
                "accessibility_settings_edu_opt_in_text",
                "accessibility_settings_edu_opt_out_text"
            )
        )

        /**
         * copy arrays, strings and preference
         */
        arrayOf(
            "arrays.xml",
            "dimens.xml",
            "strings.xml",
            "styles.xml"
        ).forEach { xmlFile ->
            copyXmlNode("youtube/settings/host", "values/$xmlFile", "resources")
        }

        val valuesV21Directory = get("res").resolve("values-v21")
        if (!valuesV21Directory.isDirectory)
            Files.createDirectories(valuesV21Directory.toPath())

        copyResources(
            "youtube/settings",
            ResourceGroup(
                "values-v21",
                "strings.xml"
            )
        )

        arrayOf(
            ResourceGroup(
                "drawable",
                "revanced_cursor.xml",
            ),
            ResourceGroup(
                "layout",
                "revanced_settings_preferences_category.xml",
                "revanced_settings_with_toolbar.xml",
            ),
            ResourceGroup(
                "xml",
                "revanced_prefs.xml",
            )
        ).forEach { resourceGroup ->
            copyResources("youtube/settings", resourceGroup)
        }

        /**
         * initialize ReVanced Extended Settings
         */
        ResourceUtils.addPreferenceFragment(
            "revanced_extended_settings",
            insertKey
        )

        /**
         * remove ReVanced Extended Settings divider
         */
        arrayOf("Theme.YouTube.Settings", "Theme.YouTube.Settings.Dark").forEach { themeName ->
            document("res/values/styles.xml").use { document ->
                with(document) {
                    val resourcesNode = getElementsByTagName("resources").item(0) as Element

                    val newElement: Element = createElement("item")
                    newElement.setAttribute("name", "android:listDivider")

                    for (i in 0 until resourcesNode.childNodes.length) {
                        val node = resourcesNode.childNodes.item(i) as? Element ?: continue

                        if (node.getAttribute("name") == themeName) {
                            newElement.appendChild(createTextNode("@null"))

                            node.appendChild(newElement)
                        }
                    }
                }
            }
        }

        /**
         * set revanced-patches version
         */
        val patchManifest = object {}.javaClass.classLoader.getResources("META-INF/MANIFEST.MF")
        while (patchManifest.hasMoreElements())
            ResourceUtils.updatePatchStatusSettings(
                "ReVanced Patches",
                Manifest(patchManifest.nextElement().openStream())
                    .mainAttributes
                    .getValue("Version") + ""
            )
    }

    finalize {
        /**
         * change RVX settings menu name
         * since it must be invoked after the Translations patch, it must be the last in the order.
         */
        if (customName != DEFAULT_LABEL) {
            removeStringsElements(
                arrayOf("revanced_extended_settings_title")
            )
            document("res/values/strings.xml").use { document ->
                mapOf(
                    "revanced_extended_settings_title" to customName
                ).forEach { (k, v) ->
                    val stringElement = document.createElement("string")

                    stringElement.setAttribute("name", k)
                    stringElement.textContent = v

                    document.getElementsByTagName("resources").item(0)
                        .appendChild(stringElement)
                }
            }
        }

        /**
         * remove summaries from RVX settings
         */
        if (settingsSummaries == false) {
            document("res/xml/revanced_prefs.xml").use { document ->
                with(document) {
                    // Get the root node of the XML document (in this case, "PreferenceScreen")
                    val rootElement = getElementsByTagName("PreferenceScreen").item(0) as Element

                    // List of attributes to remove
                    val attributesToRemove = listOf("android:summary", "android:summaryOn", "android:summaryOff")

                    // Define a recursive function to process each element
                    fun processElement(element: Element) {
                        // Skip elements with the HtmlPreference attribute to avoid errors
                        if (element.tagName == "app.revanced.extension.shared.settings.preference.HtmlPreference") {
                            return
                        }

                        // Remove specified attributes if they exist
                        attributesToRemove.forEach { attribute ->
                            if (element.hasAttribute(attribute)) {
                                element.removeAttribute(attribute)
                            }
                        }

                        // Process all child elements recursively
                        for (i in 0 until element.childNodes.length) {
                            val childNode = element.childNodes.item(i)

                            if (childNode is Element) {
                                processElement(childNode)
                            }
                        }
                    }

                    // Start processing from the root element
                    processElement(rootElement)
                }
            }

        }
    }
}
