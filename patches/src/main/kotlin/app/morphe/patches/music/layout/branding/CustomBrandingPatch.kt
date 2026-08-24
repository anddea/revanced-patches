/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
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

/*
 * Inspired by Morphe.
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.music.layout.branding

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.folderOption
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patches.music.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE_MUSIC
import app.morphe.patches.music.utils.extension.Constants.GENERAL_CLASS_DESCRIPTOR
import app.morphe.patches.music.utils.extension.sharedExtensionPatch
import app.morphe.patches.music.utils.mainactivity.mainActivityResolvePatch
import app.morphe.patches.music.utils.patch.PatchList.CUSTOM_BRANDING_FOR_YOUTUBE_MUSIC
import app.morphe.patches.music.utils.playservice.is_7_06_or_greater
import app.morphe.patches.music.utils.playservice.is_7_27_or_greater
import app.morphe.patches.music.utils.playservice.versionCheckPatch
import app.morphe.patches.music.utils.resourceid.actionBarLogoRingo2
import app.morphe.patches.music.utils.resourceid.sharedResourceIdPatch
import app.morphe.patches.music.utils.resourceid.ytmLogoRingo2
import app.morphe.patches.music.utils.settings.CategoryType
import app.morphe.patches.music.utils.settings.ResourceUtils
import app.morphe.patches.music.utils.settings.ResourceUtils.addCustomPreference
import app.morphe.patches.music.utils.settings.ResourceUtils.addListPreference
import app.morphe.patches.music.utils.settings.ResourceUtils.addSwitchPreference
import app.morphe.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.morphe.patches.music.utils.settings.settingsPatch
import app.morphe.patches.shared.layout.branding.CUSTOM_BRANDING_EXTENSION_CLASS_DESCRIPTOR
import app.morphe.patches.shared.layout.branding.BrandingIcon
import app.morphe.patches.shared.layout.branding.CustomBrandingConfig
import app.morphe.patches.shared.layout.branding.NotificationBuilderFingerprint
import app.morphe.patches.shared.layout.branding.NotificationIconFingerprint
import app.morphe.patches.shared.layout.branding.applyCustomBranding
import app.morphe.patches.shared.layout.branding.customBrandingIconOptionDescription
import app.morphe.patches.shared.layout.branding.installDynamicRvxSettingsIcon
import app.morphe.patches.shared.mainactivity.injectOnCreateMethodCall
import app.morphe.util.REGISTER_TEMPLATE_REPLACEMENT
import app.morphe.util.Utils.printWarn
import app.morphe.util.replaceLiteralInstructionCall
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

private const val ADAPTIVE_ICON_BACKGROUND_FILE_NAME =
    "adaptiveproduct_youtube_music_background_color_108"
private const val ADAPTIVE_ICON_FOREGROUND_FILE_NAME =
    "adaptiveproduct_youtube_music_foreground_color_108"
private const val ADAPTIVE_ICON_MONOCHROME_FILE_NAME =
    "ic_app_icons_themed_youtube_music"
private const val DEFAULT_ICON = "youtube_music"

private val availableIcon = listOf(
    BrandingIcon("afn_blue", "AFN Blue", true),
    BrandingIcon("afn_red", "AFN Red", true),
    BrandingIcon("mmt", "MMT", true),
    BrandingIcon("mmt_blue", "MMT Blue", true),
    BrandingIcon("mmt_green", "MMT Green", true),
    BrandingIcon("mmt_orange", "MMT Orange", true),
    BrandingIcon("mmt_pink", "MMT Pink", true),
    BrandingIcon("mmt_turquoise", "MMT Turquoise", true),
    BrandingIcon("mmt_yellow", "MMT Yellow", true),
    BrandingIcon("revancify_blue", "Revancify Blue", true),
    BrandingIcon("revancify_red", "Revancify Red", true),
    BrandingIcon("vanced_black", "Vanced Black", true),
    BrandingIcon("vanced_light", "Vanced Light", true),
    BrandingIcon("youtube_music", "YouTube Music", false),
    BrandingIcon("xisr_yellow", "Xisr Yellow", true),
)

private val brandingConfig = CustomBrandingConfig(
    resourceRoot = "music",
    adaptiveBackgroundFileName = ADAPTIVE_ICON_BACKGROUND_FILE_NAME,
    adaptiveForegroundFileName = ADAPTIVE_ICON_FOREGROUND_FILE_NAME,
    monochromeFileName = ADAPTIVE_ICON_MONOCHROME_FILE_NAME,
    settingsIconFileName = "revanced_settings_icon",
    originalSettingsIconKey = DEFAULT_ICON,
    settingsPreferencePaths = listOf("res/xml/settings_headers.xml"),
    settingsPreferenceKey = "revanced_settings",
    originalLauncherIconName = "ic_launcher_release",
    applicationNameKeys = arrayOf("app_name", "app_launcher_name"),
    originalNameFallback = "YouTube Music",
    namePresetLabels = listOf("YouTube Music", "ReVanced Extended Music", "RVX Music", "YT Music"),
    icons = availableIcon,
    mainActivityName = "com.google.android.apps.youtube.music.activities.MusicActivity",
    activityAliasNameWithIntents = "com.google.android.apps.youtube.music.activities.MusicActivity",
    copyAliasIntentFilters = false,
    useSplashlessLauncherActivity = false,
    dynamicHeaderResourceNames = listOf("action_bar_logo", "logo_music", "ytm_logo"),
    dynamicHeaderUsesThemes = false,
    dynamicSplashResourceName = "record",
    dynamicSplashResourceDirectories = listOf(
        "drawable-xlarge-hdpi",
        "drawable-xlarge-mdpi",
        "drawable-large-xhdpi",
        "drawable-large-hdpi",
        "drawable-large-mdpi",
        "drawable-xxhdpi",
        "drawable-xhdpi",
        "drawable-hdpi",
        "drawable-mdpi",
    ),
)

private val changeHeaderBytecodePatch = bytecodePatch(
    description = "changeHeaderBytecodePatch",
) {
    dependsOn(sharedResourceIdPatch, versionCheckPatch)

    execute {
        if (!is_7_06_or_greater) return@execute
        if (actionBarLogoRingo2 == -1L || ytmLogoRingo2 == -1L) {
            printWarn("Target resource not found!")
            return@execute
        }
        if (is_7_27_or_greater) {
            replaceLiteralInstructionCall(
                actionBarLogoRingo2,
                """
                    invoke-static {v$REGISTER_TEMPLATE_REPLACEMENT}, $GENERAL_CLASS_DESCRIPTOR->getHeaderDrawableId(I)I
                    move-result v$REGISTER_TEMPLATE_REPLACEMENT
                """,
            )
        }
    }
}

private val customBrandingBytecodePatch = bytecodePatch {
    dependsOn(sharedExtensionPatch, mainActivityResolvePatch)

    execute {
        injectOnCreateMethodCall(CUSTOM_BRANDING_EXTENSION_CLASS_DESCRIPTOR, "setBranding")
        injectOnCreateMethodCall(
            CUSTOM_BRANDING_EXTENSION_CLASS_DESCRIPTOR,
            "applySplashAnimation",
        )

        if (is_7_27_or_greater) {
            NotificationBuilderFingerprint.let {
                it.method.apply {
                    mapOf(
                        2 to "getColor",
                        0 to "getSmallIcon",
                    ).forEach { (offset, methodName) ->
                        val index = it.instructionMatches[offset].index
                        val register = getInstruction<FiveRegisterInstruction>(index).registerD
                        addInstructions(
                            index,
                            """
                            invoke-static { v$register }, $CUSTOM_BRANDING_EXTENSION_CLASS_DESCRIPTOR->$methodName(I)I
                            move-result v$register
                        """.trimIndent(),
                        )
                    }
                }
            }
        }

        NotificationIconFingerprint.let {
            it.method.apply {
                val index = it.instructionMatches.last().index
                val register = getInstruction<TwoRegisterInstruction>(index).registerA
                addInstructions(
                    index,
                    """
                        invoke-static { v$register }, $CUSTOM_BRANDING_EXTENSION_CLASS_DESCRIPTOR->getSmallIcon(I)I
                        move-result v$register
                    """.trimIndent(),
                )
            }
        }
    }
}

@Suppress("unused")
val customBrandingPatch = resourcePatch(
    CUSTOM_BRANDING_FOR_YOUTUBE_MUSIC.title,
    CUSTOM_BRANDING_FOR_YOUTUBE_MUSIC.summary,
    true,
) {
    compatibleWith(COMPATIBILITY_YOUTUBE_MUSIC)

    val customNameOption = stringOption(
        key = "customName",
        title = "App name",
        description = "Custom app name.",
    )
    val customIconOption = folderOption(
        key = "customIcon",
        title = "Custom icon",
        description = customBrandingIconOptionDescription,
    )

    dependsOn(
        settingsPatch,
        customBrandingBytecodePatch,
        changeHeaderBytecodePatch,
    )

    execute {
        val hasRvxSettingsPreference = applyCustomBranding(
            brandingConfig,
            customNameOption.value?.trim()?.takeIf { it.isNotEmpty() },
            customIconOption.value?.trim()?.takeIf { it.isNotEmpty() },
        )

        addListPreference(
            category = CategoryType.GENERAL.value,
            key = "morphe_custom_branding_name",
            dependencyKey = "",
            setSummary = false,
        )
        addCustomPreference(
            category = CategoryType.GENERAL.value,
            key = "morphe_custom_branding_icon",
            tag = "app.morphe.extension.shared.settings.preference.IconListPreference",
            setSummary = false,
            entriesArrayKey = "morphe_custom_branding_icon_entries",
            entryValuesArrayKey = "morphe_custom_branding_icon_entry_values",
        )
        if (hasRvxSettingsPreference) {
            installDynamicRvxSettingsIcon("revanced_settings_icon")
            addSwitchPreference(
                category = CategoryType.GENERAL.value,
                key = "morphe_custom_branding_apply_to_rvx_settings",
                defaultValue = "false",
                dependencyKey = "",
                setSummary = true,
            )
        }
        ResourceUtils.movePreferencesToTop(
            CategoryType.GENERAL.value,
            listOf(
                "morphe_custom_branding_name",
                "morphe_custom_branding_icon",
                "morphe_custom_branding_apply_to_rvx_settings",
            ),
        )

        updatePatchStatus(CUSTOM_BRANDING_FOR_YOUTUBE_MUSIC)
    }
}
