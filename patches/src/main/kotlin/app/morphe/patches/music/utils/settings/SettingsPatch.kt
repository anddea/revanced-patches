/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
 * - inotia00 (https://github.com/inotia00)
 *
 * Licensed under the GNU General Public License v3.0.
 *
 * ------------------------------------------------------------------------
 * GPLv3 Section 7 – Additional Terms & Attribution Requirements
 * ------------------------------------------------------------------------
 *
 * This file contains substantial original work by the author(s) listed above.
 *
 * In accordance with Section 7 of the GNU General Public License v3.0,
 * the following additional terms apply to this file:
 *
 * 1. Source Credit Preservation (Section 7(b)): This specific copyright notice
 *    and the list of original authors above must be preserved in any copy
 *    or derivative work. You may add your own copyright notice below it,
 *    but you may not remove the original one.
 *
 * 2. Origin & Modification Marking (Section 7(c)): Modified versions must be
 *    clearly marked as such (e.g., by adding a "Modified by" line or a new
 *    copyright notice) and must not be misrepresented as the original work.
 *
 * 3. Version Control Attribution (Section 7(b)): Any ports or substantial
 *    modifications must retain historical authorship credit in version control
 *    systems (e.g., Git), listing original author(s) appropriately and
 *    modifiers as committers or co-authors.
 *
 * 4. User Interface Attribution (Section 7(b)): Any works containing or
 *    derived from this material must maintain a visible credit or
 *    acknowledgment to the original author(s) within the application's
 *    user interface (e.g., in an "About" or "Credits" section).
 */

package app.morphe.patches.music.utils.settings

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.music.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE_MUSIC
import app.morphe.patches.music.utils.extension.Constants.EXTENSION_PATH
import app.morphe.patches.music.utils.extension.Constants.UTILS_PATH
import app.morphe.patches.music.utils.extension.sharedExtensionPatch
import app.morphe.patches.music.utils.mainactivity.mainActivityResolvePatch
import app.morphe.patches.music.utils.patch.PatchList.GMSCORE_SUPPORT
import app.morphe.patches.music.utils.patch.PatchList.SETTINGS_FOR_YOUTUBE_MUSIC
import app.morphe.patches.music.utils.playservice.is_6_39_or_greater
import app.morphe.patches.music.utils.playservice.is_8_40_or_greater
import app.morphe.patches.music.utils.playservice.versionCheckPatch
import app.morphe.patches.music.utils.settings.ResourceUtils.addGmsCorePreference
import app.morphe.patches.music.utils.settings.ResourceUtils.gmsCorePackageName
import app.morphe.patches.shared.BOLD_ICONS_FEATURE_FLAG
import app.morphe.patches.shared.boldIconsFeatureFlagMethodFingerprint
import app.morphe.patches.shared.extension.Constants.EXTENSION_THEME_UTILS_CLASS_DESCRIPTOR
import app.morphe.patches.shared.extension.Constants.EXTENSION_UTILS_CLASS_DESCRIPTOR
import app.morphe.patches.shared.mainactivity.injectConstructorMethodCall
import app.morphe.patches.shared.mainactivity.injectOnCreateMethodCall
import app.morphe.patches.shared.settings.baseSettingsPatch
import app.morphe.util.ResourceGroup
import app.morphe.util.Utils.printInfo
import app.morphe.util.copyResources
import app.morphe.util.copyXmlNode
import app.morphe.util.findMethodOrThrow
import app.morphe.util.fingerprint.matchOrThrow
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.insertLiteralOverride
import app.morphe.util.removeStringsElements
import app.morphe.util.valueOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter
import org.w3c.dom.Element

private const val EXTENSION_ACTIVITY_CLASS_DESCRIPTOR =
    "$EXTENSION_PATH/settings/ActivityHook;"
private const val EXTENSION_FRAGMENT_CLASS_DESCRIPTOR =
    "$EXTENSION_PATH/settings/preference/ReVancedPreferenceFragment;"
private const val EXTENSION_INITIALIZATION_CLASS_DESCRIPTOR =
    "$UTILS_PATH/InitializationPatch;"

private val settingsBytecodePatch = bytecodePatch(
    description = "settingsBytecodePatch"
) {
    dependsOn(
        sharedExtensionPatch,
        mainActivityResolvePatch,
        versionCheckPatch,
        baseSettingsPatch,
    )

    execute {

        // region patch for hook activity

        settingsHeadersFragmentFingerprint.matchOrThrow().let {
            it.method.apply {
                val targetIndex = it.instructionMatches.last().index
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstruction(
                    targetIndex + 1,
                    "invoke-static {v$targetRegister}, $EXTENSION_ACTIVITY_CLASS_DESCRIPTOR->setActivity(Ljava/lang/Object;)V"
                )
            }
        }

        // endregion

        // region patch for hook preference change listener

        preferenceFingerprint.matchOrThrow().let {
            it.method.apply {
                val targetIndex = it.instructionMatches.last().index
                val keyRegister = getInstruction<FiveRegisterInstruction>(targetIndex).registerD
                val valueRegister = getInstruction<FiveRegisterInstruction>(targetIndex).registerE

                addInstruction(
                    targetIndex,
                    "invoke-static {v$keyRegister, v$valueRegister}, $EXTENSION_FRAGMENT_CLASS_DESCRIPTOR->onPreferenceChanged(Ljava/lang/String;Z)V"
                )
            }
        }

        // endregion

        // region patch for hook dummy Activity for intent

        googleApiActivityFingerprint.matchOrThrow().let {
            it.method.apply {
                addInstructionsWithLabels(
                    1,
                    """
                        invoke-static {p0}, $EXTENSION_ACTIVITY_CLASS_DESCRIPTOR->initialize(Landroid/app/Activity;)Z
                        move-result v0
                        if-eqz v0, :show
                        return-void
                        """,
                    ExternalLabel("show", getInstruction(1)),
                )
            }

            it.classDef.apply {
                if (methods.none { method -> method.name == "finish" && method.parameters.isEmpty() }) {
                    ImmutableMethod(
                        type,
                        "finish",
                        emptyList<ImmutableMethodParameter>(),
                        "V",
                        AccessFlags.PUBLIC.value,
                        null,
                        null,
                        MutableMethodImplementation(3),
                    ).toMutable().apply {
                        addInstructions(
                            0,
                            """
                                invoke-static {}, $EXTENSION_ACTIVITY_CLASS_DESCRIPTOR->handleFinish()Z
                                move-result v0
                                if-nez v0, :search_handled
                                invoke-super { p0 }, $superclass->finish()V
                                :search_handled
                                return-void
                                """
                        )
                    }.let(methods::add)
                }
            }
        }

        // endregion

        // apply the current theme of the settings page
        findMethodOrThrow(EXTENSION_THEME_UTILS_CLASS_DESCRIPTOR) {
            name == "setThemeColor"
        }.addInstruction(
            0,
            "invoke-static {}, $EXTENSION_THEME_UTILS_CLASS_DESCRIPTOR->updateDarkModeStatus()V"
        )

        injectOnCreateMethodCall(
            EXTENSION_INITIALIZATION_CLASS_DESCRIPTOR,
            "onCreate"
        )
        injectConstructorMethodCall(
            EXTENSION_UTILS_CLASS_DESCRIPTOR,
            "setActivity"
        )

        // Enable Music's bold icon set after first-run initialization, matching morphe.
        if (is_8_40_or_greater) {
            boldIconsFeatureFlagMethodFingerprint.method.insertLiteralOverride(
                BOLD_ICONS_FEATURE_FLAG,
                "$EXTENSION_ACTIVITY_CLASS_DESCRIPTOR->useBoldIcons(Z)Z"
            )
        }

        accountIdentityConstructorFingerprint
            .methodOrThrow()
            .addInstruction(
                1,
                "invoke-static/range { p7 .. p7 }, $EXTENSION_INITIALIZATION_CLASS_DESCRIPTOR->" +
                        "onLoggedIn(Ljava/lang/String;)V"
            )
    }
}

private const val DEFAULT_ELEMENT = "pref_key_parent_tools"
private const val DEFAULT_LABEL = "RVX"
private const val FALLBACK_ELEMENT = "settings_header_general"
private lateinit var settingsLabel: String

private val SETTINGS_ELEMENTS_MAP = mapOf(
    "Parent settings" to DEFAULT_ELEMENT,
    "General" to FALLBACK_ELEMENT,
    "Playback" to "settings_header_playback",
    "Data saving" to "settings_header_data_saving",
    "Downloads & storage" to "settings_header_downloads_and_storage",
    "Notifications" to "settings_header_notifications",
    "Privacy & data" to "settings_header_privacy_and_location",
    "Recommendations" to "settings_header_recommendations",
    "Paid memberships" to "settings_header_paid_memberships",
    "About YouTube Music" to "settings_header_about_youtube_music",
)

val settingsPatch = resourcePatch(
    SETTINGS_FOR_YOUTUBE_MUSIC.title,
    SETTINGS_FOR_YOUTUBE_MUSIC.summary,
) {
    compatibleWith(COMPATIBILITY_YOUTUBE_MUSIC)

    dependsOn(
        settingsBytecodePatch,
        versionCheckPatch,
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

        var insertKey = insertPosition
            .valueOrThrow()

        if (!is_6_39_or_greater && insertKey == DEFAULT_ELEMENT) {
            // 'Parent settings' does not exist in YT Music 6.38.
            // Fallback to 'General'
            insertKey = FALLBACK_ELEMENT
            printInfo("Since this version does not have \"Parent settings\", patch option \"Insert position\" is replaced with \"General\".")
        }

        /**
         * copy arrays, colors, styles and strings
         */
        arrayOf(
            "arrays.xml",
            "colors.xml",
            "styles.xml",
            "strings.xml"
        ).forEach { xmlFile ->
            copyXmlNode("music/settings/host", "values/$xmlFile", "resources")
        }

        arrayOf(
            ResourceGroup(
                "drawable",
                "revanced_settings_arrow_time.xml",
                "revanced_settings_cursor.xml",
                "revanced_settings_custom_checkmark.xml",
                "revanced_settings_rounded_corners_background.xml",
                "revanced_settings_search_icon.xml",
                "revanced_settings_search_remove.xml",
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
                "revanced_preference_search_result_range_slider.xml",
                "revanced_preference_search_result_slider.xml",
                "revanced_preference_search_result_switch.xml",
                "revanced_settings_preferences_category.xml",
                "revanced_settings_with_toolbar.xml",
            ),
            ResourceGroup(
                "menu",
                "revanced_search_menu.xml",
            ),
        ).forEach { resourceGroup ->
            copyResources("youtube/settings", resourceGroup)
        }

        arrayOf(
            ResourceGroup(
                "drawable",
                "revanced_settings_toolbar_arrow_left.xml",
            ),
            ResourceGroup(
                "xml",
                "revanced_prefs.xml",
            )
        ).forEach { resourceGroup ->
            copyResources("music/settings", resourceGroup)
        }

        /**
         * hide divider
         */
        val styleFile = get("res/values/styles.xml")

        styleFile.writeText(
            styleFile.readText()
                .replace(
                    "allowDividerAbove\">true",
                    "allowDividerAbove\">false"
                ).replace(
                    "allowDividerBelow\">true",
                    "allowDividerBelow\">false"
                )
        )

        /**
         * Change colors
         */
        document("res/values/colors.xml").use { document ->
            val resourcesNode = document.getElementsByTagName("resources").item(0) as Element
            val children = resourcesNode.childNodes
            for (i in 0 until children.length) {
                val node = children.item(i) as? Element ?: continue

                node.textContent =
                    when (node.getAttribute("name")) {
                        "material_deep_teal_500",
                            -> "@android:color/white"

                        else -> continue
                    }
            }
        }

        ResourceUtils.setContext(this)
        ResourceUtils.addRVXSettingsPreference(insertKey)

        ResourceUtils.updatePatchStatus(SETTINGS_FOR_YOUTUBE_MUSIC)

        /**
         * add import export settings
         */
        addPreferenceWithIntent(
            CategoryType.MISC,
            "revanced_settings_import_export"
        )

        addSwitchPreference(
            category = CategoryType.MISC,
            key = "revanced_settings_show_slider_summaries",
            defaultValue = "true",
            dependencyKey = "",
            setSummary = true,
            titleKey = "revanced_settings_show_slider_summaries_title",
            summaryKey = "revanced_settings_show_slider_summaries_summary",
        )
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
         * add open default app settings
         */
        addPreferenceWithIntent(
            CategoryType.MISC,
            "revanced_default_app_settings"
        )

        if (GMSCORE_SUPPORT.included == true) {
            addGmsCorePreference(
                CategoryType.MISC.value,
                "gms_core_settings",
                gmsCorePackageName,
                "org.microg.gms.ui.SettingsActivity"
            )

            addSwitchPreference(
                CategoryType.MISC,
                "revanced_gms_show_dialog",
                "true"
            )
        }

        /**
         * add app info setting
         */
        addPreferenceWithIntent(
            CategoryType.MISC,
            "revanced_app_info"
        )

        /**
         * sort preference
         */
        CategoryType.entries.sorted().forEach {
            ResourceUtils.sortPreferenceCategory(it.value)
        }

        ResourceUtils.writeSearchPreferenceFile()
    }
}

internal fun addSwitchPreference(
    category: CategoryType,
    key: String,
    defaultValue: String
) = addSwitchPreference(category, key, defaultValue, "")

internal fun addSwitchPreference(
    category: CategoryType,
    key: String,
    defaultValue: String,
    setSummary: Boolean
) = addSwitchPreference(category, key, defaultValue, "", setSummary)

internal fun addSwitchPreference(
    category: CategoryType,
    key: String,
    defaultValue: String,
    dependencyKey: String
) = addSwitchPreference(category, key, defaultValue, dependencyKey, true)

internal fun addSwitchPreference(
    category: CategoryType,
    key: String,
    defaultValue: String,
    dependencyKey: String,
    setSummary: Boolean,
    titleKey: String = "${key}_title",
    summaryKey: String = "${key}_summary",
) {
    val categoryValue = category.value
    ResourceUtils.addPreferenceCategory(categoryValue)
        ResourceUtils.addSwitchPreference(
            categoryValue,
            key,
            defaultValue,
            dependencyKey,
            setSummary,
            titleKey,
            summaryKey,
        )
}

internal fun addPreferenceWithIntent(
    category: CategoryType,
    key: String,
    dependencyKey: String = "",
    setSummary: Boolean = true,
) {
    val categoryValue = category.value
    ResourceUtils.addPreferenceCategory(categoryValue)
    ResourceUtils.addPreferenceWithIntent(categoryValue, key, dependencyKey, setSummary)
}

/** Adds an extension-backed preference that handles taps in the current settings fragment. */
internal fun addCustomPreference(
    category: CategoryType,
    key: String,
    tag: String,
    dependencyKey: String = "",
    setSummary: Boolean = true,
    insertBeforeKey: String = "",
) {
    val categoryValue = category.value
    ResourceUtils.addPreferenceCategory(categoryValue)
    ResourceUtils.addCustomPreference(categoryValue, key, tag, dependencyKey, setSummary, insertBeforeKey)
}

/** Adds a dialog list preference that is handled by the current settings fragment. */
internal fun addListPreference(
    category: CategoryType,
    key: String,
    dependencyKey: String = "",
    setSummary: Boolean = true,
) {
    val categoryValue = category.value
    ResourceUtils.addPreferenceCategory(categoryValue)
    ResourceUtils.addListPreference(categoryValue, key, dependencyKey, setSummary)
}

/** Adds a text preference that is handled by the current settings fragment. */
internal fun addTextPreference(
    category: CategoryType,
    key: String,
    dependencyKey: String = "",
    setSummary: Boolean = true,
) {
    val categoryValue = category.value
    ResourceUtils.addPreferenceCategory(categoryValue)
    ResourceUtils.addTextPreference(categoryValue, key, dependencyKey, setSummary)
}

internal fun addLinkPreference(
    category: CategoryType,
    key: String,
    url: String
) {
    val categoryValue = category.value
    ResourceUtils.addPreferenceCategory(categoryValue)
    ResourceUtils.addLinkPreference(categoryValue, key, url)
}

internal fun addNonInteractivePreference(
    category: CategoryType,
    key: String,
    dependencyKey: String = "",
    setSummary: Boolean = true,
) {
    val categoryValue = category.value
    ResourceUtils.addPreferenceCategory(categoryValue)
    ResourceUtils.addNonInteractivePreference(categoryValue, key, dependencyKey, setSummary)
}
