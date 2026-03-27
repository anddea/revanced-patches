package app.morphe.patches.youtube.utils.settings

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patches.shared.extension.Constants.EXTENSION_THEME_UTILS_CLASS_DESCRIPTOR
import app.morphe.patches.shared.extension.Constants.EXTENSION_UTILS_CLASS_DESCRIPTOR
import app.morphe.patches.shared.mainactivity.injectConstructorMethodCall
import app.morphe.patches.shared.mainactivity.injectOnCreateMethodCall
import app.morphe.patches.shared.settings.baseSettingsPatch
import app.morphe.patches.youtube.utils.cairoFragmentConfigFingerprint
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.youtube.utils.extension.Constants.UTILS_PATH
import app.morphe.patches.youtube.utils.extension.sharedExtensionPatch
import app.morphe.patches.youtube.utils.fix.attributes.themeAttributesPatch
import app.morphe.patches.youtube.utils.fix.playbackspeed.playbackSpeedWhilePlayingPatch
import app.morphe.patches.youtube.utils.fix.splash.darkModeSplashScreenPatch
import app.morphe.patches.youtube.utils.mainactivity.mainActivityResolvePatch
import app.morphe.patches.youtube.utils.patch.PatchList.SETTINGS_FOR_YOUTUBE
import app.morphe.patches.youtube.utils.playservice.is_19_34_or_greater
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import app.morphe.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.morphe.patches.youtube.utils.settings.ResourceUtils.YOUTUBE_SETTINGS_PATH
import app.morphe.patches.youtube.utils.settingsFragmentSyntheticFingerprint
import app.morphe.util.FilesCompat
import app.morphe.util.ResourceGroup
import app.morphe.util.Utils.printWarn
import app.morphe.util.addInstructionsAtControlFlowLabel
import app.morphe.util.copyResources
import app.morphe.util.copyXmlNode
import app.morphe.util.findElementByAttributeValueOrThrow
import app.morphe.util.findInstructionIndicesReversedOrThrow
import app.morphe.util.fingerprint.methodCall
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.insertNode
import app.morphe.util.removeStringsElements
import app.morphe.util.returnEarly
import app.morphe.util.valueOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter
import com.android.tools.smali.dexlib2.util.MethodUtil
import org.w3c.dom.Element
import java.nio.file.Files

private const val EXTENSION_INITIALIZATION_CLASS_DESCRIPTOR =
    "$UTILS_PATH/InitializationPatch;"

private const val EXTENSION_THEME_METHOD_DESCRIPTOR =
    "$EXTENSION_THEME_UTILS_CLASS_DESCRIPTOR->updateLightDarkModeStatus(Ljava/lang/Enum;)V"

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/morphe/extension/youtube/settings/YouTubeActivityHook;"

private lateinit var bytecodeContext: BytecodePatchContext

internal fun getBytecodeContext() = bytecodeContext

internal var cairoFragmentDisabled = false

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

        licenseActivityOnCreateFingerprint.let {
            val superClass = it.classDef.superclass

            it.method.addInstructions(
                0,
                """
                    # Some targets have extra instructions before the call to super method.
                    invoke-super { p0, p1 }, $superClass->onCreate(Landroid/os/Bundle;)V
                    invoke-static { p0 }, $EXTENSION_CLASS_DESCRIPTOR->initialize(Landroid/app/Activity;)V
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
            // Add attachBaseContext method to override the context for setting a specific language.
            ImmutableMethod(
                type,
                "attachBaseContext",
                listOf(ImmutableMethodParameter("Landroid/content/Context;", annotations, null)),
                "V",
                AccessFlags.PROTECTED.value,
                null,
                null,
                MutableMethodImplementation(3),
            ).toMutable().apply {
                addInstructions(
                    """
                        invoke-static { p1 }, $EXTENSION_CLASS_DESCRIPTOR->getAttachBaseContext(Landroid/content/Context;)Landroid/content/Context;
                        move-result-object p1
                        invoke-super { p0, p1 }, $superclass->attachBaseContext(Landroid/content/Context;)V
                        return-void
                    """
                )
            }.let(methods::add)

            // Override finish() to intercept back gesture.
            ImmutableMethod(
                licenseActivityOnCreateFingerprint.classDef.type,
                "finish",
                emptyList(),
                "V",
                AccessFlags.PUBLIC.value,
                null,
                null,
                MutableMethodImplementation(3),
            ).toMutable().apply {
                addInstructions(
                    """
                        invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->handleBackPress()Z
                        move-result v0
                        if-nez v0, :search_handled
                        invoke-super { p0 }, Landroid/app/Activity;->finish()V
                        return-void
                        :search_handled
                        return-void
                    """
                )
            }.let(methods::add)
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
                "revanced_settings_cursor.xml",
                "revanced_settings_custom_checkmark.xml",
                "revanced_settings_icon.xml",
                "revanced_settings_rounded_corners_background.xml",
                "revanced_settings_search_icon.xml",
                "revanced_settings_search_remove.xml",
                "revanced_settings_toolbar_arrow_left.xml",
            ),
            ResourceGroup(
                "layout",
                "revanced_color_dot_widget.xml",
                "revanced_color_picker.xml",
                "revanced_custom_list_item_checked.xml",
                "revanced_preference_search_history_item.xml",
                "revanced_preference_search_history_screen.xml",
                "revanced_preference_search_no_result.xml",
                "revanced_preference_search_result_color.xml",
                "revanced_preference_search_result_group_header.xml",
                "revanced_preference_search_result_list.xml",
                "revanced_preference_search_result_regular.xml",
                "revanced_preference_search_result_switch.xml",
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
         * initialize ReVanced Extended Settings
         */
        ResourceUtils.addPreferenceFragment(
            "revanced_settings",
            insertKey,
            "com.google.android.libraries.social.licenses.LicenseActivity"
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
                arrayOf("revanced_settings_title")
            )
            document("res/values/strings.xml").use { document ->
                mapOf(
                    "revanced_settings_title" to settingsLabel
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
    }
}
