package app.revanced.patches.youtube.utils.settings

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatchContext
import app.revanced.patcher.patch.booleanOption
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patcher.patch.stringOption
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.shared.extension.Constants.EXTENSION_THEME_UTILS_CLASS_DESCRIPTOR
import app.revanced.patches.shared.extension.Constants.EXTENSION_UTILS_CLASS_DESCRIPTOR
import app.revanced.patches.shared.mainactivity.injectConstructorMethodCall
import app.revanced.patches.shared.mainactivity.injectOnCreateMethodCall
import app.revanced.patches.shared.settings.baseSettingsPatch
import app.revanced.patches.youtube.utils.cairoFragmentConfigFingerprint
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.extension.Constants.PATCH_STATUS_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.extension.Constants.UTILS_PATH
import app.revanced.patches.youtube.utils.extension.sharedExtensionPatch
import app.revanced.patches.youtube.utils.fix.attributes.themeAttributesPatch
import app.revanced.patches.youtube.utils.fix.playbackspeed.playbackSpeedWhilePlayingPatch
import app.revanced.patches.youtube.utils.fix.splash.darkModeSplashScreenPatch
import app.revanced.patches.youtube.utils.mainactivity.mainActivityResolvePatch
import app.revanced.patches.youtube.utils.patch.PatchList.SETTINGS_FOR_YOUTUBE
import app.revanced.patches.youtube.utils.playservice.is_19_15_or_greater
import app.revanced.patches.youtube.utils.playservice.is_19_34_or_greater
import app.revanced.patches.youtube.utils.playservice.versionCheckPatch
import app.revanced.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.ResourceUtils.YOUTUBE_SETTINGS_PATH
import app.revanced.patches.youtube.utils.settingsFragmentSyntheticFingerprint
import app.revanced.util.FilesCompat
import app.revanced.util.ResourceGroup
import app.revanced.util.Utils.printWarn
import app.revanced.util.addInstructionsAtControlFlowLabel
import app.revanced.util.className
import app.revanced.util.copyResources
import app.revanced.util.copyXmlNode
import app.revanced.util.findFreeRegister
import app.revanced.util.findInstructionIndicesReversedOrThrow
import app.revanced.util.findMethodOrThrow
import app.revanced.util.fingerprint.methodCall
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.fingerprint.mutableClassOrThrow
import app.revanced.util.getReference
import app.revanced.util.hookClassHierarchy
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.insertNode
import app.revanced.util.removeStringsElements
import app.revanced.util.returnEarly
import app.revanced.util.valueOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.util.MethodUtil
import org.w3c.dom.Element
import java.nio.file.Files

private const val EXTENSION_INITIALIZATION_CLASS_DESCRIPTOR =
    "$UTILS_PATH/InitializationPatch;"

private const val EXTENSION_THEME_METHOD_DESCRIPTOR =
    "$EXTENSION_THEME_UTILS_CLASS_DESCRIPTOR->updateLightDarkModeStatus(Ljava/lang/Enum;)V"

private lateinit var bytecodeContext: BytecodePatchContext

internal fun getBytecodeContext() = bytecodeContext

internal var cairoFragmentDisabled = false
private var targetActivityClassName = ""

private val settingsBytecodePatch = bytecodePatch(
    description = "settingsBytecodePatch"
) {
    dependsOn(
        sharedExtensionPatch,
        sharedResourceIdPatch,
        mainActivityResolvePatch,
        versionCheckPatch,
        baseSettingsPatch,
    )

    execute {
        bytecodeContext = this

        // region fix cairo fragment.

        /**
         * Disable Cairo fragment settings.
         * 1. Fix - When spoofing the app version to 19.20 or earlier, the app crashes or the Notifications tab is inaccessible.
         * 2. Fix - Preference 'Playback' is hidden.
         * 3. Some settings that were in Preference 'General' are moved to Preference 'Playback'.
         */
        // Cairo fragment have been widely rolled out in YouTube 19.34+.
        if (is_19_34_or_greater) {
            // Instead of disabling all Cairo fragment configs,
            // Just disable 'Load Cairo fragment xml' and 'Set style to Cairo preference'.
            fun MutableMethod.disableCairoFragmentConfig() {
                val cairoFragmentConfigMethodCall = cairoFragmentConfigFingerprint
                    .methodCall()
                val insertIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            getReference<MethodReference>()?.toString() == cairoFragmentConfigMethodCall
                } + 2
                val insertRegister =
                    getInstruction<OneRegisterInstruction>(insertIndex - 1).registerA

                addInstruction(insertIndex, "const/4 v$insertRegister, 0x0")
            }

            try {
                arrayOf(
                    // Load cairo fragment xml.
                    settingsFragmentSyntheticFingerprint
                        .methodOrThrow(),
                    // Set style to cairo preference.
                    settingsFragmentStylePrimaryFingerprint
                        .methodOrThrow(),
                    settingsFragmentStyleSecondaryFingerprint
                        .methodOrThrow(settingsFragmentStylePrimaryFingerprint),
                ).forEach { method ->
                    method.disableCairoFragmentConfig()
                }
                cairoFragmentDisabled = true
            } catch (_: Exception) {
                cairoFragmentConfigFingerprint
                    .methodOrThrow()
                    .returnEarly()

                printWarn("Failed to restore 'Playback' settings. 'Autoplay next video' setting may not appear in the YouTube settings.")
            }
        }

        // endregion.

        val targetActivityFingerprint = if (is_19_15_or_greater)
            proxyBillingActivityV2OnCreateFingerprint
        else
            licenseMenuActivityOnCreateFingerprint

        val hostActivityClass = settingsHostActivityOnCreateFingerprint.mutableClassOrThrow()
        val targetActivityClass = targetActivityFingerprint.mutableClassOrThrow()

        hookClassHierarchy(
            hostActivityClass,
            targetActivityClass
        )

        targetActivityClass.methods.forEach { method ->
            method.apply {
                if (!MethodUtil.isConstructor(method) && returnType == "V") {
                    val insertIndex =
                        indexOfFirstInstruction(Opcode.INVOKE_SUPER) + 1
                    if (insertIndex > 0) {
                        val freeRegister = findFreeRegister(insertIndex)

                        addInstructionsWithLabels(
                            insertIndex, """
                                invoke-virtual {p0}, ${hostActivityClass.type}->isInitialized()Z
                                move-result v$freeRegister
                                if-eqz v$freeRegister, :ignore
                                return-void
                                :ignore
                                nop
                                """
                        )
                    }
                }
            }
        }

        targetActivityClassName = targetActivityClass.type.className
        findMethodOrThrow(PATCH_STATUS_CLASS_DESCRIPTOR) {
            name == "TargetActivityClass"
        }.returnEarly(targetActivityClassName)

        // apply the current theme of the settings page
        themeSetterSystemFingerprint.methodOrThrow().apply {
            findInstructionIndicesReversedOrThrow(Opcode.RETURN_OBJECT).forEach { index ->
                val register = getInstruction<OneRegisterInstruction>(index).registerA

                addInstructionsAtControlFlowLabel(
                    index,
                    "invoke-static { v$register }, $EXTENSION_THEME_METHOD_DESCRIPTOR"
                )
            }
        }

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

private const val DEFAULT_ELEMENT = "@string/parent_tools_key"
private const val DEFAULT_LABEL = "RVX"

private val SETTINGS_ELEMENTS_MAP = mapOf(
    "Parent settings" to DEFAULT_ELEMENT,
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
    "About" to "@string/about_key"
)

private lateinit var settingsLabel: String

val settingsPatch = resourcePatch(
    SETTINGS_FOR_YOUTUBE.title,
    SETTINGS_FOR_YOUTUBE.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsBytecodePatch,
        darkModeSplashScreenPatch,
        playbackSpeedWhilePlayingPatch,
        themeAttributesPatch,
    )

    val insertPosition = stringOption(
        key = "insertPosition",
        default = DEFAULT_ELEMENT,
        values = SETTINGS_ELEMENTS_MAP,
        title = "Insert position",
        description = "The settings menu name that the RVX settings menu should be above.",
        required = true,
    )

    val rvxSettingsLabel = stringOption(
        key = "rvxSettingsLabel",
        default = DEFAULT_LABEL,
        values = mapOf(
            "ReVanced Extended" to "ReVanced Extended",
            "RVX" to DEFAULT_LABEL,
        ),
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
        settingsLabel = rvxSettingsLabel
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
                "revanced_settings_arrow_time.xml",
                "revanced_settings_circle_background.xml",
                "revanced_settings_cursor.xml",
                "revanced_settings_custom_checkmark.xml",
                "revanced_settings_rounded_corners_background.xml",
                "revanced_settings_search_icon.xml",
                "revanced_settings_toolbar_arrow_left.xml",
            ),
            ResourceGroup(
                "layout",
                "revanced_color_dot_widget.xml",
                "revanced_color_picker.xml",
                "revanced_custom_list_item_checked.xml",
                "revanced_preference_with_icon_no_search_result.xml",
                "revanced_search_suggestion_item.xml",
                "revanced_settings_preferences_category.xml",
                "revanced_settings_with_toolbar.xml",
            ),
            ResourceGroup(
                "menu",
                "revanced_search_menu.xml",
            ),
            ResourceGroup(
                "xml",
                "revanced_prefs.xml",
            )
        ).forEach { resourceGroup ->
            copyResources("youtube/settings", resourceGroup)
        }

        /**
         * add searchDependency attribute to group parent and children settings
         */
        document("res/values/attrs.xml").use { document ->
            (document.getElementsByTagName("resources").item(0) as Element).appendChild(
                document.createElement("attr").apply {
                    setAttribute("format", "string")
                    setAttribute("name", "searchDependency")
                }
            )
        }

        /**
         * initialize ReVanced Extended Settings
         */
        ResourceUtils.addPreferenceFragment(
            "revanced_extended_settings",
            insertKey,
            targetActivityClassName,
        )

        /**
         * remove ReVanced Extended Settings divider
         */
        document("res/values/styles.xml").use { document ->
            val themeNames = arrayOf("Theme.YouTube.Settings", "Theme.YouTube.Settings.Dark")
            with(document) {
                val resourcesNode = documentElement
                val childNodes = resourcesNode.childNodes

                for (i in 0 until childNodes.length) {
                    val node = childNodes.item(i) as? Element ?: continue

                    if (node.getAttribute("name") in themeNames) {
                        val newElement = createElement("item").apply {
                            setAttribute("name", "android:listDivider")
                            appendChild(createTextNode("@null"))
                        }
                        node.appendChild(newElement)
                    }
                }
            }
        }
    }

    finalize {
        /**
         * change RVX settings menu name
         * since it must be invoked after the Translations patch, it must be the last in the order.
         */
        if (settingsLabel != DEFAULT_LABEL) {
            removeStringsElements(
                arrayOf("revanced_extended_settings_title")
            )
            document("res/values/strings.xml").use { document ->
                mapOf(
                    "revanced_extended_settings_title" to settingsLabel
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
         * Disable Cairo fragment settings.
         */
        if (cairoFragmentDisabled) {
            /**
             * If the app version is spoofed to 19.30 or earlier due to the Spoof app version patch,
             * the 'Playback' setting will be broken.
             * If the app version is spoofed, the previous fragment must be used.
             */
            val xmlDirectory = get("res").resolve("xml")
            FilesCompat.copy(
                xmlDirectory.resolve("settings_fragment.xml"),
                xmlDirectory.resolve("settings_fragment_legacy.xml")
            )

            /**
             * The Preference key for 'Playback' is '@string/playback_key'.
             * Copy the node to add the Preference 'Playback' to the legacy settings fragment.
             */
            document(YOUTUBE_SETTINGS_PATH).use { document ->
                val tags = document.getElementsByTagName("Preference")
                List(tags.length) { tags.item(it) as Element }
                    .find { it.getAttribute("android:key") == "@string/auto_play_key" }
                    ?.let { node ->
                        node.insertNode("Preference", node) {
                            for (index in 0 until node.attributes.length) {
                                with(node.attributes.item(index)) {
                                    setAttribute(nodeName, nodeValue)
                                }
                            }
                            setAttribute("android:key", "@string/playback_key")
                        }
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
