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

package app.morphe.patches.youtube.layout.branding

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.folderOption
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patches.shared.layout.branding.CUSTOM_BRANDING_EXTENSION_CLASS_DESCRIPTOR
import app.morphe.patches.shared.layout.branding.BrandingIcon
import app.morphe.patches.shared.layout.branding.CustomBrandingConfig
import app.morphe.patches.shared.layout.branding.NotificationBuilderFingerprint
import app.morphe.patches.shared.layout.branding.NotificationIconFingerprint
import app.morphe.patches.shared.layout.branding.applyCustomBranding
import app.morphe.patches.shared.layout.branding.customBrandingIconOptionDescription
import app.morphe.patches.shared.layout.branding.installYouTubeRvxSettingsIconLayout
import app.morphe.patches.shared.mapping.ResourceType
import app.morphe.patches.shared.mapping.resourceLiteral
import app.morphe.patches.shared.mainactivity.injectOnCreateMethodCall
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.utils.extension.sharedExtensionPatch
import app.morphe.patches.youtube.general.toolbar.attributeResolverFingerprint
import app.morphe.patches.youtube.utils.mainactivity.mainActivityResolvePatch
import app.morphe.patches.youtube.utils.patch.PatchList.CUSTOM_BRANDING_FOR_YOUTUBE
import app.morphe.patches.youtube.utils.playservice.is_20_00_or_greater
import app.morphe.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.util.findInstructionIndicesReversed
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

private const val ADAPTIVE_ICON_BACKGROUND_FILE_NAME =
    "adaptiveproduct_youtube_background_color_108"
private const val ADAPTIVE_ICON_FOREGROUND_FILE_NAME =
    "adaptiveproduct_youtube_foreground_color_108"
private const val ADAPTIVE_ICON_MONOCHROME_FILE_NAME =
    "adaptive_monochrome_ic_youtube_launcher"
private const val DEFAULT_ICON = "youtube"

private val availableIcon = listOf(
    BrandingIcon("afn_blue", "@string/revanced_icon_afn_blue", true),
    BrandingIcon("afn_red", "@string/revanced_icon_afn_red", true),
    BrandingIcon("mmt", "@string/revanced_icon_mmt", true),
    BrandingIcon("mmt_blue", "@string/revanced_icon_mmt_blue", true),
    BrandingIcon("mmt_green", "@string/revanced_icon_mmt_green", true),
    BrandingIcon("mmt_orange", "@string/revanced_icon_mmt_orange", true),
    BrandingIcon("mmt_pink", "@string/revanced_icon_mmt_pink", true),
    BrandingIcon("mmt_turquoise", "@string/revanced_icon_mmt_turquoise", true),
    BrandingIcon("mmt_yellow", "@string/revanced_icon_mmt_yellow", true),
    BrandingIcon("revancify_blue", "@string/revanced_icon_revancify_blue", true),
    BrandingIcon("revancify_red", "@string/revanced_icon_revancify_red", true),
    BrandingIcon("squid_game", "@string/revanced_icon_squid_game", true),
    BrandingIcon("vanced_black", "@string/revanced_icon_vanced_black", hasAdaptiveLayers = true, hasMonochromeLayers = false),
    BrandingIcon("vanced_light", "@string/revanced_icon_vanced_light", hasAdaptiveLayers = true, hasMonochromeLayers = false),
    BrandingIcon("xisr_aurora", "@string/revanced_icon_xisr_aurora", true),
    BrandingIcon("xisr_evergreen", "@string/revanced_icon_xisr_evergreen", true),
    BrandingIcon("xisr_special", "@string/revanced_icon_xisr_special", true),
    BrandingIcon("xisr_white", "@string/revanced_icon_xisr_white", true),
    BrandingIcon("xisr_winter", "@string/revanced_icon_xisr_winter", true),
    BrandingIcon("xisr_yellow", "@string/revanced_icon_xisr_yellow", true),
    BrandingIcon("youtube", "@string/revanced_icon_youtube", false),
    BrandingIcon("youtube_black", "@string/revanced_icon_youtube_black", hasAdaptiveLayers = true, hasMonochromeLayers = false),
)

private val brandingConfig = CustomBrandingConfig(
    resourceRoot = "youtube",
    adaptiveBackgroundFileName = ADAPTIVE_ICON_BACKGROUND_FILE_NAME,
    adaptiveForegroundFileName = ADAPTIVE_ICON_FOREGROUND_FILE_NAME,
    monochromeFileName = ADAPTIVE_ICON_MONOCHROME_FILE_NAME,
    settingsIconFileName = "revanced_settings_key_icon",
    originalSettingsIconKey = DEFAULT_ICON,
    settingsPreferencePaths = listOf(
        "res/xml/settings_fragment.xml",
        "res/xml/settings_fragment_cairo.xml",
        "res/xml/settings_fragment_legacy.xml",
    ),
    settingsPreferenceKey = "revanced_settings_key",
    originalLauncherIconName = "ic_launcher",
    applicationNameKeys = arrayOf("application_name"),
    originalNameFallback = "YouTube",
    namePresetLabels = listOf("YouTube", "ReVanced Extended", "RVX", "YouTube RVX"),
    icons = availableIcon,
    mainActivityName = "com.google.android.apps.youtube.app.watchwhile.MainActivity",
    activityAliasNameWithIntents = $$"com.google.android.youtube.app.honeycomb.Shell$HomeActivity",
    copyAliasIntentFilters = true,
    dynamicHeaderResourceNames = listOf("yt_wordmark_header", "yt_premium_wordmark_header"),
    dynamicSplashResourceName = "product_logo_youtube_color_144",
)

private val applicationNameFingerprint = Fingerprint(
    filters = listOf(resourceLiteral(ResourceType.STRING, "application_name")),
)

private val customBrandingBytecodePatch = bytecodePatch {
    dependsOn(sharedExtensionPatch, mainActivityResolvePatch, sharedResourceIdPatch)

    execute {
        injectOnCreateMethodCall(CUSTOM_BRANDING_EXTENSION_CLASS_DESCRIPTOR, "setBranding")
        injectOnCreateMethodCall(CUSTOM_BRANDING_EXTENSION_CLASS_DESCRIPTOR, "applySplashAnimation")

        // application_name is used by YouTube for client metadata and several in-app labels. The
        // manifest aliases still provide the launcher fallback, while this hook makes the chosen
        // preset visible to code that resolves the application name at runtime.
        applicationNameFingerprint.matchAll().forEach { match ->
            match.method.apply {
                match.instructionMatches
                    .map { it.index }
                    .distinct()
                    .sortedDescending()
                    .forEach { literalIndex ->
                        val resultIndex = indexOfFirstInstructionOrThrow(
                            literalIndex,
                            Opcode.MOVE_RESULT_OBJECT,
                        )
                        val register = getInstruction<OneRegisterInstruction>(resultIndex).registerA
                        addInstructions(
                            resultIndex + 1,
                            """
                                invoke-static {v$register}, $CUSTOM_BRANDING_EXTENSION_CLASS_DESCRIPTOR->getApplicationName(Ljava/lang/String;)Ljava/lang/String;
                                move-result-object v$register
                            """.trimIndent(),
                        )
                    }
            }
        }

        // All supported YouTube versions funnel premium and non-premium wordmarks through this
        // shared resolver. Wrapping each return covers every header caller without modifying its
        // registers. The extension leaves unrelated drawable attributes untouched.
        attributeResolverFingerprint.methodOrThrow().apply {
            findInstructionIndicesReversed { opcode == Opcode.RETURN_OBJECT }.forEach { index ->
                val register = getInstruction<OneRegisterInstruction>(index).registerA
                addInstructions(
                    index,
                    """
                        invoke-static {p1, v$register}, $CUSTOM_BRANDING_EXTENSION_CLASS_DESCRIPTOR->getHeaderDrawable(ILandroid/graphics/drawable/Drawable;)Landroid/graphics/drawable/Drawable;
                        move-result-object v$register
                    """.trimIndent(),
                )
            }
        }

        if (is_20_00_or_greater) {
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
    CUSTOM_BRANDING_FOR_YOUTUBE.title,
    CUSTOM_BRANDING_FOR_YOUTUBE.summary,
    true,
) {
    compatibleWith(COMPATIBILITY_YOUTUBE)

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
    )

    execute {
        applyCustomBranding(
            brandingConfig,
            customNameOption.value?.trim()?.takeIf { it.isNotEmpty() },
            customIconOption.value?.trim()?.takeIf { it.isNotEmpty() },
        )
        installYouTubeRvxSettingsIconLayout()
        addPreference(
            arrayOf("SETTINGS: CUSTOM_BRANDING"),
            CUSTOM_BRANDING_FOR_YOUTUBE,
        )
    }
}
