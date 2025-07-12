package app.revanced.patches.youtube.utils.settings

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.*
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patches.shared.extension.Constants.EXTENSION_THEME_UTILS_CLASS_DESCRIPTOR
import app.revanced.patches.shared.extension.Constants.EXTENSION_UTILS_CLASS_DESCRIPTOR
import app.revanced.patches.shared.mainactivity.injectConstructorMethodCall
import app.revanced.patches.shared.mainactivity.injectOnCreateMethodCall
import app.revanced.patches.shared.settings.baseSettingsPatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.extension.Constants.UTILS_PATH
import app.revanced.patches.youtube.utils.extension.sharedExtensionPatch
import app.revanced.patches.youtube.utils.fix.attributes.themeAttributesPatch
import app.revanced.patches.youtube.utils.fix.cairo.cairoFragmentPatch
import app.revanced.patches.youtube.utils.fix.playbackspeed.playbackSpeedWhilePlayingPatch
import app.revanced.patches.youtube.utils.fix.splash.darkModeSplashScreenPatch
import app.revanced.patches.youtube.utils.mainactivity.mainActivityResolvePatch
import app.revanced.patches.youtube.utils.patch.PatchList.SETTINGS_FOR_YOUTUBE
import app.revanced.patches.youtube.utils.playservice.versionCheckPatch
import app.revanced.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.revanced.util.*
import app.revanced.util.fingerprint.methodOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter
import com.android.tools.smali.dexlib2.util.MethodUtil
import org.w3c.dom.Element
import java.nio.file.Files

private const val EXTENSION_INITIALIZATION_CLASS_DESCRIPTOR =
    "$UTILS_PATH/InitializationPatch;"

private const val EXTENSION_THEME_METHOD_DESCRIPTOR =
    "$EXTENSION_THEME_UTILS_CLASS_DESCRIPTOR->setTheme(Ljava/lang/Enum;)V"

private lateinit var bytecodeContext: BytecodePatchContext

internal fun getBytecodeContext() = bytecodeContext

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

        // Modify the license activity and remove all existing layout code.
        // Must modify an existing activity and cannot add a new activity to the manifest,
        // as that fails for root installations.

        val extensionPackage = "app/revanced/extension/youtube"
        val activityHookClassDescriptor = "L$extensionPackage/settings/LicenseActivityHook;"
        val fragmentClassDescriptor = "L$extensionPackage/settings/preference/ReVancedPreferenceFragment;"

        licenseActivityOnCreateFingerprint.let {
            val superClass = it.classDef.superclass

            it.method.addInstructions(
                0,
                """
                    # Some targets have extra instructions before the call to super method.
                    invoke-super { p0, p1 }, $superClass->onCreate(Landroid/os/Bundle;)V
                    invoke-static { p0 }, $activityHookClassDescriptor->initialize(Landroid/app/Activity;)V
                    return-void
                """
            )
        }

        // Remove other methods as they will break as the onCreate method is modified above.
        licenseActivityOnCreateFingerprint.classDef.apply {
            methods.removeIf { it.name != "onCreate" && !MethodUtil.isConstructor(it) }
        }

        // Add onBackPressed to handle system back presses and gestures.
        licenseActivityOnCreateFingerprint.classDef.apply {
            val onBackPressed = ImmutableMethod(
                type,
                "onBackPressed",
                emptyList(),
                "V",
                AccessFlags.PUBLIC.value,
                null,
                null,
                MutableMethodImplementation(4),
            ).toMutable().apply {
                addInstructions(
                    """
                        # Get fragment from LicenseActivityHook
                        sget-object v0, $activityHookClassDescriptor->fragmentRef:Ljava/lang/ref/WeakReference;
                        invoke-virtual { v0 }, Ljava/lang/ref/WeakReference;->get()Ljava/lang/Object;
                        move-result-object v0
                        check-cast v0, $fragmentClassDescriptor

                        # If fragment is null, call super
                        if-eqz v0, :super_on_back_pressed

                        # Get SearchView query or empty string
                        sget-object v1, $activityHookClassDescriptor->searchViewRef:Ljava/lang/ref/WeakReference;
                        invoke-virtual { v1 }, Ljava/lang/ref/WeakReference;->get()Ljava/lang/Object;
                        move-result-object v1
                        check-cast v1, Landroid/widget/SearchView;

                        # Assume query is initially empty string if searchView is null
                        const-string v2, ""
                        if-eqz v1, :call_handle_on_back_pressed_with_empty_query

                        # If searchView is NOT null, get its query
                        invoke-virtual { v1 }, Landroid/widget/SearchView;->getQuery()Ljava/lang/CharSequence;
                        move-result-object v1
                        invoke-interface { v1 }, Ljava/lang/CharSequence;->toString()Ljava/lang/String;
                        move-result-object v2

                        :call_handle_on_back_pressed_with_empty_query
                        invoke-virtual { v0, v2 }, $fragmentClassDescriptor->handleOnBackPressed(Ljava/lang/String;)Z
                        move-result v0

                        # If handleOnBackPressed returned TRUE, call super.onBackPressed()
                        if-nez v0, :super_on_back_pressed
                        return-void

                        :super_on_back_pressed
                        invoke-super { p0 }, $superclass->onBackPressed()V
                        return-void
                    """
                )
            }

            methods.add(onBackPressed)
        }

        // Add context override to force a specific settings language.
        licenseActivityOnCreateFingerprint.classDef.apply {
            val attachBaseContext = ImmutableMethod(
                type,
                "attachBaseContext",
                listOf(ImmutableMethodParameter("Landroid/content/Context;", annotations, null)), "V",
                AccessFlags.PROTECTED.value,
                null,
                null,
                MutableMethodImplementation(3),
            ).toMutable().apply {
                addInstructions(
                    """
                        invoke-static { p1 }, $activityHookClassDescriptor->getAttachBaseContext(Landroid/content/Context;)Landroid/content/Context;
                        move-result-object p1
                        invoke-super { p0, p1 }, $superclass->attachBaseContext(Landroid/content/Context;)V
                        return-void
                    """
                )
            }

            methods.add(attachBaseContext)
        }
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
        cairoFragmentPatch,
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
                "revanced_cursor.xml",
                "revanced_settings_custom_checkmark.xml",
                "revanced_settings_rounded_corners_background.xml",
            ),
            ResourceGroup(
                "layout",
                "revanced_color_picker.xml",
                "revanced_custom_list_item_checked.xml",
                "revanced_preference_with_icon_no_search_result.xml",
                "revanced_search_suggestion_item.xml",
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
            insertKey
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

        // Modify the manifest and add a data intent filter to the LicenseActivity.
        // Some devices freak out if undeclared data is passed to an intent,
        // and this change appears to fix the issue.
        document("AndroidManifest.xml").use { document ->
            val licenseElement = document.childNodes.findElementByAttributeValueOrThrow(
                "android:name",
                "com.google.android.libraries.social.licenses.LicenseActivity",
            )

            val mimeType = document.createElement("data")
            mimeType.setAttribute("android:mimeType", "text/plain")

            val intentFilter = document.createElement("intent-filter")
            intentFilter.appendChild(mimeType)

            licenseElement.appendChild(intentFilter)
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
