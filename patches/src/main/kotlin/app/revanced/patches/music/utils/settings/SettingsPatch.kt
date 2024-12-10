package app.revanced.patches.music.utils.settings

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patcher.patch.stringOption
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.extension.Constants.EXTENSION_PATH
import app.revanced.patches.music.utils.extension.Constants.UTILS_PATH
import app.revanced.patches.music.utils.extension.sharedExtensionPatch
import app.revanced.patches.music.utils.mainactivity.mainActivityResolvePatch
import app.revanced.patches.music.utils.patch.PatchList.SETTINGS_FOR_YOUTUBE_MUSIC
import app.revanced.patches.music.utils.playservice.versionCheckPatch
import app.revanced.patches.shared.extension.Constants.EXTENSION_UTILS_CLASS_DESCRIPTOR
import app.revanced.patches.shared.mainactivity.injectConstructorMethodCall
import app.revanced.patches.shared.mainactivity.injectOnCreateMethodCall
import app.revanced.patches.shared.sharedSettingFingerprint
import app.revanced.util.copyXmlNode
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.removeStringsElements
import app.revanced.util.valueOrThrow
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
        versionCheckPatch,
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
                val targetIndex = it.patternMatch!!.endIndex
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
                val targetIndex = it.patternMatch!!.endIndex
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

        injectOnCreateMethodCall(
            EXTENSION_INITIALIZATION_CLASS_DESCRIPTOR,
            "setDeviceInformation"
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

private const val DEFAULT_LABEL = "ReVanced Extended"
private lateinit var customName: String

val settingsPatch = resourcePatch(
    SETTINGS_FOR_YOUTUBE_MUSIC.title,
    SETTINGS_FOR_YOUTUBE_MUSIC.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsBytecodePatch,
    )

    val settingsLabel = stringOption(
        key = "settingsLabel",
        default = DEFAULT_LABEL,
        title = "RVX settings label",
        description = "The name of the RVX settings menu.",
        required = true,
    )

    execute {
        /**
         * check patch options
         */
        customName = settingsLabel
            .valueOrThrow()

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
        ResourceUtils.addRVXSettingsPreference()

        ResourceUtils.updatePatchStatus(SETTINGS_FOR_YOUTUBE_MUSIC)
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
         * add open default app settings
         */
        addPreferenceWithIntent(
            CategoryType.MISC,
            "revanced_default_app_settings"
        )

        /**
         * add import export settings
         */
        addPreferenceWithIntent(
            CategoryType.MISC,
            "revanced_extended_settings_import_export"
        )

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
    key: String
) = addPreferenceWithIntent(category, key, "")

internal fun addPreferenceWithIntent(
    category: CategoryType,
    key: String,
    dependencyKey: String
) {
    val categoryValue = category.value
    ResourceUtils.addPreferenceCategory(categoryValue)
    ResourceUtils.addPreferenceWithIntent(categoryValue, key, dependencyKey)
}

