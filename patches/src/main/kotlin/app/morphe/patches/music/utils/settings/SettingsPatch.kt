package app.morphe.patches.music.utils.settings

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.music.utils.extension.Constants.EXTENSION_PATH
import app.morphe.patches.music.utils.extension.Constants.UTILS_PATH
import app.morphe.patches.music.utils.extension.sharedExtensionPatch
import app.morphe.patches.music.utils.fix.timedlyrics.timedLyricsPatch
import app.morphe.patches.music.utils.mainactivity.mainActivityResolvePatch
import app.morphe.patches.music.utils.patch.PatchList.GMSCORE_SUPPORT
import app.morphe.patches.music.utils.patch.PatchList.SETTINGS_FOR_YOUTUBE_MUSIC
import app.morphe.patches.music.utils.playservice.is_6_39_or_greater
import app.morphe.patches.music.utils.playservice.is_6_42_or_greater
import app.morphe.patches.music.utils.playservice.versionCheckPatch
import app.morphe.patches.music.utils.settings.ResourceUtils.addGmsCorePreference
import app.morphe.patches.music.utils.settings.ResourceUtils.gmsCorePackageName
import app.morphe.patches.shared.extension.Constants.EXTENSION_THEME_UTILS_CLASS_DESCRIPTOR
import app.morphe.patches.shared.extension.Constants.EXTENSION_UTILS_CLASS_DESCRIPTOR
import app.morphe.patches.shared.mainactivity.injectConstructorMethodCall
import app.morphe.patches.shared.mainactivity.injectOnCreateMethodCall
import app.morphe.patches.shared.settings.baseSettingsPatch
import app.morphe.patches.shared.sharedSettingFingerprint
import app.morphe.util.ResourceGroup
import app.morphe.util.Utils.printInfo
import app.morphe.util.copyResources
import app.morphe.util.copyXmlNode
import app.morphe.util.findMethodOrThrow
import app.morphe.util.fingerprint.matchOrThrow
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.removeStringsElements
import app.morphe.util.valueOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
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
        timedLyricsPatch,
        versionCheckPatch,
        baseSettingsPatch,
    )

    execute {

        // region patch for set SharedPrefCategory

        sharedSettingFingerprint.methodOrThrow().apply {
            val stringIndex = indexOfFirstInstructionOrThrow(Opcode.CONST_STRING)
            val stringRegister = getInstruction<OneRegisterInstruction>(stringIndex).registerA

            replaceInstruction(
                stringIndex,
                "const-string v$stringRegister, \"youtube\""
            )
        }

        // endregion

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

        googleApiActivityFingerprint.methodOrThrow().apply {
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
    compatibleWith(COMPATIBLE_PACKAGE)

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
         * copy arrays, colors and strings
         */
        arrayOf(
            "arrays.xml",
            "colors.xml",
            "strings.xml"
        ).forEach { xmlFile ->
            copyXmlNode("music/settings/host", "values/$xmlFile", "resources")
        }

        arrayOf(
            ResourceGroup(
                "drawable",
                "revanced_settings_toolbar_arrow_left.xml",
            ),
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
         * Html.fromHtml() requires Android 7.0+
         */
        if (is_6_42_or_greater) {
            addPreferenceWithIntent(
                CategoryType.MISC,
                "revanced_app_info"
            )
        }

        /**
         * sort preference
         */
        CategoryType.entries.sorted().forEach {
            ResourceUtils.sortPreferenceCategory(it.value)
        }
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
    setSummary: Boolean
) {
    val categoryValue = category.value
    ResourceUtils.addPreferenceCategory(categoryValue)
        ResourceUtils.addSwitchPreference(categoryValue, key, defaultValue, dependencyKey, setSummary)
}

internal fun addPreferenceWithIntent(
    category: CategoryType,
    key: String,
    dependencyKey: String = ""
) {
    val categoryValue = category.value
    ResourceUtils.addPreferenceCategory(categoryValue)
    ResourceUtils.addPreferenceWithIntent(categoryValue, key, dependencyKey)
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
