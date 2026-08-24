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
 * Portions of this file are ported from Morphe:
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/2524
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.layout.theme

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.ResourcePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.shared.extension.Constants.EXTENSION_UTILS_CLASS_DESCRIPTOR
import app.morphe.patches.shared.mapping.resourceMappingPatch
import app.morphe.patches.youtube.general.navigation.PivotBarBuilderFingerprint
import app.morphe.patches.youtube.general.splashanimation.splashScreenAnimationBytecodePatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.utils.extension.Constants.PATCHES_PATH
import app.morphe.patches.youtube.utils.extension.hooks.applicationInitHook
import app.morphe.patches.youtube.utils.mainactivity.mainActivityResolvePatch
import app.morphe.patches.youtube.utils.patch.PatchList.THEME
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.ResourceUtils.updatePatchStatusTheme
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.patches.shared.mainactivity.injectOnCreateMethodCall
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import org.w3c.dom.Element

private const val THEME_EXTENSION_CLASS_DESCRIPTOR = "$PATCHES_PATH/theme/ThemePatch;"
private const val THEME_APPLICATION_METHOD_DESCRIPTOR =
    "$THEME_EXTENSION_CLASS_DESCRIPTOR->setTheme(Landroid/content/Context;)V"
private const val EXTENSION_SET_CONTEXT_METHOD_DESCRIPTOR =
    "$EXTENSION_UTILS_CLASS_DESCRIPTOR->setContext(Landroid/content/Context;)V"
private const val RUNTIME_LIGHT_THEME_COLOR = "morphe_runtime_light_theme_color"
private const val RUNTIME_LIGHT_THEME_COLOR_OPACITY70 = "morphe_runtime_light_theme_color_opacity70"
private const val RUNTIME_LIGHT_THEME_COLOR_ID = "0x7f060f0f"
private const val RUNTIME_LIGHT_THEME_COLOR_OPACITY70_ID = "0x7f060f10"
private const val SPLASH_THEME_DARK_PREFIX = "morphe_theme_splash_dark_"
private const val SPLASH_THEME_LIGHT_PREFIX = "morphe_theme_splash_light_"
private const val SPLASH_THEME_NO_ICON_SUFFIX = "_no_icon"
private const val SPLASH_THEME_PARENT = "@style/Theme.YouTube.Home"
private const val PRECOMPILED_THEME_MCC = 801
private const val DEFAULT_LIGHT_THEME_COLOR = "#FFFFFFFF"
private const val NAVIGATION_CONTENT_COUNT_METHOD = "getContentCountId"
private const val NAVIGATION_CONTENT_DOT_METHOD = "getContentDotId"

/** Neutral colors shared by player scrims and the player-status ambient layer. */
private val stockPlayerColorNames = setOf(
    "yt_black_pure",
    "yt_black_pure_opacity80",
    "yt_black_pure_opacity60",
    "yt_white1_opacity70",
)

private fun MutableMethod.addNewContentIndicatorHook(checkCastIndex: Int) {
    val stubRegister = getInstruction<OneRegisterInstruction>(checkCastIndex).registerA
    addInstruction(
        checkCastIndex + 1,
        "invoke-static { v$stubRegister }, $THEME_EXTENSION_CLASS_DESCRIPTOR" +
                "->onNewContentIndicator(Landroid/view/ViewStub;)V",
    )
}

private val stockDarkThemeColors = linkedMapOf(
    "yt_black0" to "#FF282828",
    "yt_black1" to "#FF212121",
    "yt_black1_opacity95" to "#F2212121",
    "yt_black1_opacity98" to "#FA212121",
    "yt_black2" to "#FF181818",
    "yt_black3" to "#FF0F0F0F",
    "yt_black4" to "#FF030303",
    "yt_status_bar_background_dark" to "#FF131313",
    "material_grey_850" to "#FF303030",
    "yt_black_pure" to "#FF000000",
    "material_grey_800" to "#FF424242",
    "material_grey_900" to "#FF212121",
    "yt_black0_opacity60" to "#99282828",
    "yt_black_pure_opacity80" to "#CC000000",
    "yt_black_pure_opacity60" to "#99000000",
)

private val runtimeDarkThemeResources = stockDarkThemeColors.keys.associateWith { name -> "morphe_runtime_dark_theme_${name.removePrefix("yt_")}" }

private val runtimeThemeResourceIds =
    runtimeDarkThemeResources.values.mapIndexed { index, name ->
        name to "0x7f06${(0xf00 + index).toString(16).padStart(4, '0')}"
    }.toMap() + mapOf(
        RUNTIME_LIGHT_THEME_COLOR to RUNTIME_LIGHT_THEME_COLOR_ID,
        RUNTIME_LIGHT_THEME_COLOR_OPACITY70 to RUNTIME_LIGHT_THEME_COLOR_OPACITY70_ID,
    )

private fun lightThemeColorWithOpacity70(color: String): String {
    val hex = color.removePrefix("#")
    return when (hex.length) {
        6 -> "#B3$hex"
        8 -> "#B3${hex.substring(2)}"
        else -> "#B3FFFFFF"
    }
}

/** Preserves each stock resource's alpha while replacing its RGB channels. */
private fun darkThemeColorWithStockAlpha(color: String, stockColor: String): String {
    val colorHex = color.removePrefix("#")
    val stockHex = stockColor.removePrefix("#")
    if (colorHex.length !in setOf(6, 8) || stockHex.length != 8) return color

    return "#${stockHex.substring(0, 2)}${colorHex.takeLast(6)}"
}

private val darkThemeKeys = listOf(
    "amoled_black", "material_you_neutral", "material_you_primary",
    "material_you_secondary", "material_you_tertiary", "modern_youtube",
    "classic_youtube", "catppuccin_mocha", "dark_pink", "dark_blue",
    "dark_green", "dark_yellow", "dark_orange", "dark_red",
)

private val lightThemeKeys = listOf(
    "white", "material_you_neutral", "material_you_primary",
    "material_you_secondary", "material_you_tertiary", "catppuccin_latte",
    "light_pink", "light_blue", "light_green", "light_yellow", "light_orange",
    "light_red", "pale_blue", "pale_green", "pale_yellow",
)

private val precompiledDarkThemeColors = mapOf(
    "amoled_black" to "#FF000000",
    "modern_youtube" to "#FF0F0F0F",
    "classic_youtube" to "#FF212121",
    "catppuccin_mocha" to "#FF181825",
    "dark_pink" to "#FF290025",
    "dark_blue" to "#FF001029",
    "dark_green" to "#FF002905",
    "dark_yellow" to "#FF282900",
    "dark_orange" to "#FF291800",
    "dark_red" to "#FF290000",
)

private val precompiledLightThemeColors = mapOf(
    "white" to "#FFFFFFFF",
    "catppuccin_latte" to "#FFE6E9EF",
    "light_pink" to "#FFFCCFF3",
    "light_blue" to "#FFD1E0FF",
    "light_green" to "#FFCCFFCC",
    "light_yellow" to "#FFFDFFCC",
    "light_orange" to "#FFFFE6CC",
    "light_red" to "#FFFFD6D6",
    "pale_blue" to "#FFD4FFF8",
    "pale_green" to "#FFD1FFCC",
    "pale_yellow" to "#FFFFE9AA",
)

/** Static colors used by Android's system process when it draws the starting window. */
private val splashDarkThemeColors = linkedMapOf(
    "stock" to stockDarkThemeColors.getValue("yt_black3"),
    "amoled_black" to precompiledDarkThemeColors.getValue("amoled_black"),
    "material_you_neutral" to "@android:color/system_neutral1_900",
    "material_you_primary" to "@android:color/system_accent1_800",
    "material_you_secondary" to "@android:color/system_accent2_800",
    "material_you_tertiary" to "@android:color/system_accent3_800",
    "modern_youtube" to precompiledDarkThemeColors.getValue("modern_youtube"),
    "classic_youtube" to precompiledDarkThemeColors.getValue("classic_youtube"),
    "catppuccin_mocha" to precompiledDarkThemeColors.getValue("catppuccin_mocha"),
    "dark_pink" to precompiledDarkThemeColors.getValue("dark_pink"),
    "dark_blue" to precompiledDarkThemeColors.getValue("dark_blue"),
    "dark_green" to precompiledDarkThemeColors.getValue("dark_green"),
    "dark_yellow" to precompiledDarkThemeColors.getValue("dark_yellow"),
    "dark_orange" to precompiledDarkThemeColors.getValue("dark_orange"),
    "dark_red" to precompiledDarkThemeColors.getValue("dark_red"),
    // A custom color is resolved by the app process, so use the stock fallback for the system splash.
    "custom" to stockDarkThemeColors.getValue("yt_black3"),
)

private val splashLightThemeColors = linkedMapOf(
    "white" to DEFAULT_LIGHT_THEME_COLOR,
    "material_you_neutral" to "@android:color/system_neutral1_100",
    "material_you_primary" to "@android:color/system_accent1_200",
    "material_you_secondary" to "@android:color/system_accent2_200",
    "material_you_tertiary" to "@android:color/system_accent3_200",
    "catppuccin_latte" to precompiledLightThemeColors.getValue("catppuccin_latte"),
    "light_pink" to precompiledLightThemeColors.getValue("light_pink"),
    "light_blue" to precompiledLightThemeColors.getValue("light_blue"),
    "light_green" to precompiledLightThemeColors.getValue("light_green"),
    "light_yellow" to precompiledLightThemeColors.getValue("light_yellow"),
    "light_orange" to precompiledLightThemeColors.getValue("light_orange"),
    "light_red" to precompiledLightThemeColors.getValue("light_red"),
    "pale_blue" to precompiledLightThemeColors.getValue("pale_blue"),
    "pale_green" to precompiledLightThemeColors.getValue("pale_green"),
    "pale_yellow" to precompiledLightThemeColors.getValue("pale_yellow"),
    // A custom color is resolved by the app process, so use the stock fallback for the system splash.
    "custom" to DEFAULT_LIGHT_THEME_COLOR,
)

private val runtimeThemeBytecodePatch = bytecodePatch(
    description = "runtimeThemeBytecodePatch",
) {
    dependsOn(settingsPatch, mainActivityResolvePatch)

    execute {
        // Keep the runtime theme hook owned by Theme so it is applied whenever this patch is selected.
        injectOnCreateMethodCall(THEME_EXTENSION_CLASS_DESCRIPTOR, "setTheme")

        // Prepare the overlay before the first Activity. The shared extension hook initializes its
        // Context at the beginning of Application.onCreate, so insert immediately after that
        // call. The Activity hook then applies the selected starting-window theme before onCreate
        // invokes the original Activity implementation.
        val applicationOnCreate = applicationInitHook.fingerprint.method
        val contextHookIndex = applicationOnCreate.implementation?.instructions
            ?.indexOfFirst {
                it.getReference<MethodReference>()?.toString() ==
                        EXTENSION_SET_CONTEXT_METHOD_DESCRIPTOR
            }
            ?: -1
        if (contextHookIndex < 0) {
            throw PatchException("Could not find the shared extension context hook")
        }
        applicationOnCreate.addInstruction(
            contextHookIndex + 1,
            "invoke-static { p0 }, $THEME_APPLICATION_METHOD_DESCRIPTOR"
        )
    }
}

/** Hooks the pivot-bar and notification-button indicators so Material You can keep their color. */
val newContentIndicatorBytecodePatch = bytecodePatch(
    description = "newContentIndicatorBytecodePatch",
) {
    dependsOn(settingsPatch, resourceMappingPatch)

    execute {
        // The pivot bar has a stub for both the dot and the count next to it.
        PivotBarNewContentDotFingerprint.matchOrNull()?.let {
            // Hook the count first so the earlier dot match remains valid.
            it.method.addNewContentIndicatorHook(it.instructionMatches.last().index)
            it.method.addNewContentIndicatorHook(it.instructionMatches[2].index)
        } ?: PivotBarBuilderFingerprint.matchOrNull()?.method?.let { method ->
            // NavigationBarComponentsPatch replaces these findViewById calls with extension
            // methods before this patch runs. In that form the original fingerprint cannot match,
            // but the returned ViewStub and its cast remain unchanged.
            val checkCastIndices = method.implementation?.instructions
                ?.mapIndexedNotNull { index, instruction ->
                    val reference = instruction.getReference<MethodReference>()
                    if (instruction.opcode == Opcode.INVOKE_STATIC &&
                        reference?.name in setOf(
                            NAVIGATION_CONTENT_COUNT_METHOD,
                            NAVIGATION_CONTENT_DOT_METHOD,
                        )
                    ) {
                        method.indexOfFirstInstructionOrThrow(index, Opcode.CHECK_CAST)
                    } else {
                        null
                    }
                }
                ?.distinct()
                ?.sortedDescending()
                ?: emptyList()

            if (checkCastIndices.size < 2) {
                throw PatchException("Could not find both pivot bar new content indicators")
            }

            checkCastIndices.forEach(method::addNewContentIndicatorHook)
        } ?: throw PatchException("Could not find the pivot bar new content indicators")

        // The notification button of the top bar has a separate pair of indicators.
        TopBarNewContentCountFingerprint.let {
            it.method.apply {
                // Hook the dot first so the count match remains valid after insertion.
                arrayOf(
                    it.instructionMatches.last().index,
                    it.instructionMatches[2].index,
                ).forEach { checkCastIndex ->
                    addNewContentIndicatorHook(checkCastIndex)
                }
            }
        }
    }
}

@Suppress("unused")
val themePatch = resourcePatch(
    THEME.title,
    THEME.summary,
) {
    compatibleWith(COMPATIBILITY_YOUTUBE)

    dependsOn(
        sharedThemePatch,
        settingsPatch,
        splashScreenAnimationBytecodePatch,
        runtimeThemeBytecodePatch,
        newContentIndicatorBytecodePatch,
    )

    execute {
        val existingColorResourceNames = document("res/values/public.xml").use { document ->
            val publicNodes = document.getElementsByTagName("public")
            (0 until publicNodes.length)
                .map { publicNodes.item(it) as Element }
                .filter { it.getAttribute("type") == "color" }
                .map { it.getAttribute("name") }
                .toSet()
        }

        // Keep YouTube's neutral player colors stock globally. The status-bar fallback shares the
        // translucent entries, so its selected RGB is applied only at the bytecode fingerprint.
        val precompiledDarkThemeResources = stockDarkThemeColors.filterKeys { name ->
            name !in stockPlayerColorNames && name in existingColorResourceNames
        }

        val precompiledLightThemeResources = setOf(
            "yt_white1",
            "yt_white1_opacity95",
            "yt_white1_opacity98",
            "yt_white2",
            "yt_white3",
            "yt_white4",
            "material_grey_50",
            "material_grey_100",
        ).filter(existingColorResourceNames::contains)
        // Android 8–10 have no public ResourcesLoader API. Precompile every dark/light pair under
        // one synthetic MCC and a pair-specific MNC, then select concrete resources before
        // inflation. Keeping the MCC synthetic prevents these variants from matching real mobile
        // networks on Android 11+, where the runtime loader remains authoritative. The qualified
        // files override existing app entries because arsclib cannot introduce a new resource-table
        // entry from a qualified-only definition.
        val precompiledDarkThemeKeys = listOf("stock") + darkThemeKeys
        val precompiledLightThemeKeys = lightThemeKeys
        precompiledDarkThemeKeys.forEachIndexed { darkIndex, darkKey ->
            val selectedDarkColor =
                if (darkKey == "stock") null else precompiledDarkThemeColors[darkKey]
            precompiledLightThemeKeys.forEachIndexed { lightIndex, lightKey ->
                val selectedLightColor =
                    precompiledLightThemeColors[lightKey] ?: DEFAULT_LIGHT_THEME_COLOR
                val mnc = darkIndex * precompiledLightThemeKeys.size + lightIndex + 1
                val directory =
                    "values-mcc$PRECOMPILED_THEME_MCC-mnc${mnc.toString().padStart(3, '0')}"
                // ARSCLib derives the values resource type from the XML filename.
                val path = "res/$directory/colors.xml"
                get(path).apply {
                    parentFile?.mkdirs()
                    writeText("<?xml version=\"1.0\" encoding=\"utf-8\"?><resources />")
                }
                document(path).use { document ->
                    val resourcesNode = document.documentElement
                    precompiledDarkThemeResources.forEach { (stockName, stockColor) ->
                        resourcesNode.appendChild(document.createElement("color").apply {
                            setAttribute("name", stockName)
                            textContent = selectedDarkColor?.let {
                                darkThemeColorWithStockAlpha(
                                    it,
                                    stockColor,
                                )
                            } ?: stockColor
                        })
                    }
                    precompiledLightThemeResources.forEach { name ->
                        resourcesNode.appendChild(document.createElement("color").apply {
                            setAttribute("name", name)
                            textContent = selectedLightColor
                        })
                    }
                }
            }
        }

        document("res/values/public.xml").use { document ->
            val reservedIds = runtimeThemeResourceIds.values.toSet()
            val publicNodes = document.getElementsByTagName("public")
            if ((0 until publicNodes.length)
                    .map { publicNodes.item(it) as Element }
                    .any { it.getAttribute("id") in reservedIds }) {
                throw PatchException("Reserved runtime theme resource ID is already in use")
            }
            runtimeThemeResourceIds.forEach { (name, id) ->
                document.documentElement.appendChild(document.createElement("public").apply {
                    setAttribute("type", "color")
                    setAttribute("name", name)
                    setAttribute("id", id)
                })
            }
        }

        arrayOf("values", "values-v31").forEach { path ->
            document("res/$path/colors.xml").use { document ->
                val resourcesNode = document.documentElement
                val childNodes = resourcesNode.childNodes

                // Runtime colors are deliberately defined only in the base configuration. A v31
                // definition outranks ResourcesLoader on Android 12+ and forces the fallback color.
                if (path == "values") {
                    runtimeDarkThemeResources.forEach { (stockName, runtimeName) ->
                        resourcesNode.appendChild(document.createElement("color").apply {
                            setAttribute("name", runtimeName)
                            textContent = stockDarkThemeColors.getValue(stockName)
                        })
                    }
                    resourcesNode.appendChild(document.createElement("color").apply {
                        setAttribute("name", RUNTIME_LIGHT_THEME_COLOR)
                        textContent = DEFAULT_LIGHT_THEME_COLOR
                    })
                    resourcesNode.appendChild(document.createElement("color").apply {
                        setAttribute("name", RUNTIME_LIGHT_THEME_COLOR_OPACITY70)
                        textContent = lightThemeColorWithOpacity70(DEFAULT_LIGHT_THEME_COLOR)
                    })
                }

                for (i in 0 until childNodes.length) {
                    val node = childNodes.item(i) as? Element ?: continue

                    node.textContent = when (node.getAttribute("name")) {
                        in stockPlayerColorNames -> continue

                        in runtimeDarkThemeResources ->
                            "@color/${runtimeDarkThemeResources.getValue(node.getAttribute("name"))}"

                        "yt_white1", "yt_white1_opacity95", "yt_white1_opacity98",
                        "yt_white2", "yt_white3", "yt_white4",
                        "material_grey_50", "material_grey_100" ->
                            "@color/$RUNTIME_LIGHT_THEME_COLOR"

                        else -> continue
                    }
                }
            }
        }

        addSplashThemes()

        updatePatchStatusTheme("Stock + MaterialYou")

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "SETTINGS: RUNTIME_THEME",
                "SETTINGS: SPLASH_SCREEN_ANIMATION_STYLE",
            )
        )

    }
}

/**
 * Adds stable starting-window themes for every built-in app-theme preset.
 *
 * The system creates the original splash screen before Application.onCreate, so the runtime
 * resource overlay cannot affect the manifest theme in time. These styles contain concrete colors
 * that the system process can resolve before the app process starts; the Activity hook selects the
 * matching style after installing the existing overlay.
 */
private fun ResourcePatchContext.addSplashThemes() {
    val themes = listOf(
        SPLASH_THEME_DARK_PREFIX to splashDarkThemeColors,
        SPLASH_THEME_LIGHT_PREFIX to splashLightThemeColors,
    )

    listOf(
        "res/values/styles.xml" to false,
        "res/values-v31/styles.xml" to true,
    ).forEach { (path, includeSplashBackground) ->
        val stylesFile = get(path)
        if (!stylesFile.exists()) {
            stylesFile.parentFile?.mkdirs()
            stylesFile.writeText("<?xml version=\"1.0\" encoding=\"utf-8\"?><resources />")
        }

        document(path).use { document ->
            val resources = document.documentElement

            themes.forEach { (themePrefix, themeColors) ->
                themeColors.forEach { (themeKey, color) ->
                    listOf("" to false, SPLASH_THEME_NO_ICON_SUFFIX to true).forEach {
                        (iconSuffix, hideSplashIcon) ->
                        val themeName = themePrefix + themeKey + iconSuffix
                        (0 until resources.childNodes.length)
                            .map { resources.childNodes.item(it) }
                            .filterIsInstance<Element>()
                            .filter {
                                it.tagName == "style" && it.getAttribute("name") == themeName
                            }
                            .forEach(resources::removeChild)

                        val style = document.createElement("style").apply {
                            setAttribute("name", themeName)
                            setAttribute("parent", SPLASH_THEME_PARENT)
                        }

                        fun addItem(name: String, value: String = color) {
                            style.appendChild(document.createElement("item").apply {
                                setAttribute("name", name)
                                textContent = value
                            })
                        }

                        addItem("android:windowBackground")
                        if (includeSplashBackground) {
                            addItem("android:windowSplashScreenBackground")
                            if (hideSplashIcon) {
                                addItem(
                                    "android:windowSplashScreenAnimatedIcon",
                                    "@android:color/transparent",
                                )
                                addItem("android:windowSplashScreenAnimationDuration", "0")
                            }
                        }
                        resources.appendChild(style)
                    }
                }
            }
        }
    }
}
