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

package app.morphe.patches.music.layout.theme

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.ResourcePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.music.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE_MUSIC
import app.morphe.patches.music.utils.extension.Constants.PATCH_STATUS_CLASS_DESCRIPTOR
import app.morphe.patches.music.utils.extension.Constants.UTILS_PATH
import app.morphe.patches.music.utils.mainactivity.mainActivityResolvePatch
import app.morphe.patches.music.utils.patch.PatchList.DARK_THEME
import app.morphe.patches.music.utils.resourceid.sharedResourceIdPatch
import app.morphe.patches.music.utils.settings.ResourceUtils
import app.morphe.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.morphe.patches.music.utils.settings.addCustomPreference
import app.morphe.patches.music.utils.settings.addListPreference
import app.morphe.patches.music.utils.settings.CategoryType
import app.morphe.patches.music.utils.settings.settingsPatch
import app.morphe.patches.shared.drawable.addDrawableColorHook
import app.morphe.patches.shared.drawable.drawableColorHookPatch
import app.morphe.patches.shared.mainactivity.injectOnCreateMethodCall
import app.morphe.util.ResourceGroup
import app.morphe.util.copyResources
import app.morphe.util.findMethodOrThrow
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import org.w3c.dom.Element

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$UTILS_PATH/DrawableColorPatch;"
private const val SPLASH_THEME_PREFIX = "morphe_theme_splash_"
private const val SPLASH_THEME_NO_ICON_SUFFIX = "_no_icon"
private const val SPLASH_THEME_PARENT = "@style/Theme.YouTubeMusic"
private const val PRECOMPILED_THEME_QUALIFIER_BASE = 801
private const val DEFAULT_DARK_THEME_COLOR = "#FF0F0F0F"

// These are the native YT Music dark-palette values. Runtime colors use stable IDs so Android
// 11+ can switch palettes without changing the app's version-specific resource IDs.
private val stockDarkThemeColors = linkedMapOf(
    "yt_black0" to "#FF282828",
    "yt_black1" to "#FF212121",
    "yt_black1_opacity95" to "#F2212121",
    "yt_black1_opacity98" to "#FA212121",
    "yt_black2" to "#FF181818",
    "yt_black3" to "#FF0F0F0F",
    "yt_black4" to "#FF030303",
    "yt_black_pure" to "#FF000000",
    "yt_black_pure_opacity80" to "#CC000000",
    "yt_status_bar_background_dark" to "#FF131313",
    "ytm_color_grey_12" to "#FF1D1D1D",
    "material_grey_800" to "#FF424242",
    "material_grey_850" to "#FF303030",
)

private val runtimeDarkThemeResources =
    stockDarkThemeColors.keys.associateWith { name -> "morphe_runtime_dark_theme_${name.removePrefix("yt_")}" }

private val runtimeThemeResourceIds = runtimeDarkThemeResources.values
    .mapIndexed { index, name ->
        name to "0x7f06${(0xf00 + index).toString(16).padStart(4, '0')}"
    }
    .toMap()

private val darkThemeKeys = listOf(
    "amoled_black",
    "material_you_neutral",
    "material_you_primary",
    "material_you_secondary",
    "material_you_tertiary",
    "modern_youtube",
    "classic_youtube",
    "catppuccin_mocha",
    "dark_pink",
    "dark_blue",
    "dark_green",
    "dark_yellow",
    "dark_orange",
    "dark_red",
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

/** Static colors used by Android's system process when it draws the Music starting window. */
private val splashThemeColors = linkedMapOf(
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
    "custom" to DEFAULT_DARK_THEME_COLOR,
)

/** Preserves each stock resource's alpha while replacing its RGB channels. */
private fun darkThemeColorWithStockAlpha(color: String, stockColor: String): String {
    val colorHex = color.removePrefix("#")
    val stockHex = stockColor.removePrefix("#")
    if (colorHex.length !in setOf(6, 8) || stockHex.length != 8) return color

    return "#${stockHex.substring(0, 2)}${colorHex.takeLast(6)}"
}

private val darkThemeBytecodePatch = bytecodePatch(
    description = "darkThemeBytecodePatch"
) {
    dependsOn(
        settingsPatch,
        sharedResourceIdPatch,
        drawableColorHookPatch,
        mainActivityResolvePatch,
    )

    execute {
        injectOnCreateMethodCall(EXTENSION_CLASS_DESCRIPTOR, "setTheme")
        addDrawableColorHook("$EXTENSION_CLASS_DESCRIPTOR->getLithoColor(I)I")

        // The images in the playlist and album headers have a black gradient (probably applied server-side).
        // Applies a new gradient to the images in the playlist and album headers.
        elementsContainerFingerprint.methodOrThrow().apply {
            val index = indexOfFirstInstructionReversedOrThrow(Opcode.CHECK_CAST)
            val register = getInstruction<OneRegisterInstruction>(index).registerA

            addInstruction(
                index + 1,
                "invoke-static {v$register}, $EXTENSION_CLASS_DESCRIPTOR->setHeaderGradient(Landroid/view/ViewGroup;)V"
            )
        }

        findMethodOrThrow(PATCH_STATUS_CLASS_DESCRIPTOR) {
            name == "DarkTheme"
        }.replaceInstruction(
            0,
            "const/4 v0, 0x1"
        )
    }
}

@Suppress("unused")
val darkThemePatch = resourcePatch(
    DARK_THEME.title,
    DARK_THEME.summary,
) {
    compatibleWith(COMPATIBILITY_YOUTUBE_MUSIC)

    dependsOn(darkThemeBytecodePatch)

    execute {
        val existingColorResourceNames = document("res/values/public.xml").use { document ->
            val publicNodes = document.getElementsByTagName("public")
            (0 until publicNodes.length)
                .map { publicNodes.item(it) as Element }
                .filter { it.getAttribute("type") == "color" }
                .map { it.getAttribute("name") }
                .toSet()
        }
        val precompiledDarkThemeResources = stockDarkThemeColors
            .filterKeys(existingColorResourceNames::contains)

        // Android 8–10 have no public ResourcesLoader API. Precompile each fixed palette under
        // a synthetic MCC qualifier, then select those concrete resources before inflation. The
        // qualified files override existing app entries because arsclib cannot introduce a new
        // resource-table entry from a qualified-only definition.
        (listOf("stock") + darkThemeKeys).forEachIndexed { index, key ->
            val directory = "values-mcc${PRECOMPILED_THEME_QUALIFIER_BASE + index}"
            // ARSCLib derives the values resource type from the XML filename.
            val path = "res/$directory/colors.xml"
            get(path).apply {
                parentFile?.mkdirs()
                writeText("<?xml version=\"1.0\" encoding=\"utf-8\"?><resources />")
            }
            document(path).use { document ->
                val selectedColor = if (key == "stock") null else precompiledDarkThemeColors[key]
                precompiledDarkThemeResources.forEach { (stockName, stockColor) ->
                    document.documentElement.appendChild(document.createElement("color").apply {
                        setAttribute("name", stockName)
                        textContent = selectedColor?.let {
                            darkThemeColorWithStockAlpha(it, stockColor)
                        } ?: stockColor
                    })
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
            if (!get("res/$path/colors.xml").exists()) return@forEach

            document("res/$path/colors.xml").use { document ->
                val resourcesNode = document.documentElement
                val childNodes = resourcesNode.childNodes

                if (path == "values") {
                    runtimeDarkThemeResources.forEach { (_, runtimeName) ->
                        resourcesNode.appendChild(document.createElement("color").apply {
                            setAttribute("name", runtimeName)
                            textContent = DEFAULT_DARK_THEME_COLOR
                        })
                    }
                }

                for (i in 0 until childNodes.length) {
                    val node = childNodes.item(i) as? Element ?: continue
                    val colorName = node.getAttribute("name")
                    if (colorName in runtimeDarkThemeResources) {
                        node.textContent = "@color/${runtimeDarkThemeResources.getValue(colorName)}"
                    }
                }
            }
        }

        arrayOf(
            ResourceGroup(
                "drawable",
                "revanced_header_gradient.xml",
            )
        ).forEach { resourceGroup ->
            copyResources("music/theme", resourceGroup)
        }

        addSplashTheme()

        addListPreference(CategoryType.GENERAL, "morphe_dark_theme", setSummary = false)
        addCustomPreference(
            CategoryType.GENERAL,
            "morphe_dark_theme_custom_color",
            "app.morphe.extension.shared.settings.preference.ColorPickerPreference",
        )
        ResourceUtils.movePreferencesToTop(
            CategoryType.GENERAL.value,
            listOf(
                "morphe_custom_branding_name",
                "morphe_custom_branding_icon",
                "morphe_custom_branding_apply_to_rvx_settings",
                "morphe_dark_theme",
                "morphe_dark_theme_custom_color",
            ),
        )

        updatePatchStatus(DARK_THEME)

    }
}

/**
 * Adds stable starting-window styles for every built-in Music dark-theme preset.
 *
 * Android 12 creates the original splash before the runtime resource overlay is installed. The
 * generated styles therefore keep concrete colors for the system process, while the Activity
 * hook selects the matching style on the next launch.
 */
private fun ResourcePatchContext.addSplashTheme() {
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

            splashThemeColors.forEach { (themeKey, color) ->
                listOf("" to false, SPLASH_THEME_NO_ICON_SUFFIX to true).forEach {
                    (iconSuffix, hideSplashIcon) ->
                    val themeName = SPLASH_THEME_PREFIX + themeKey + iconSuffix
                    (0 until resources.childNodes.length)
                        .map { resources.childNodes.item(it) }
                        .filterIsInstance<Element>()
                        .filter { it.tagName == "style" && it.getAttribute("name") == themeName }
                        .forEach(resources::removeChild)

                    val style = document.createElement("style").apply {
                        setAttribute("name", themeName)
                        setAttribute("parent", SPLASH_THEME_PARENT)
                    }

                    style.appendChild(document.createElement("item").apply {
                        setAttribute("name", "android:windowBackground")
                        textContent = color
                    })
                    if (includeSplashBackground) {
                        style.appendChild(document.createElement("item").apply {
                            setAttribute("name", "android:windowSplashScreenBackground")
                            textContent = color
                        })
                        if (hideSplashIcon) {
                            style.appendChild(document.createElement("item").apply {
                                setAttribute("name", "android:windowSplashScreenAnimatedIcon")
                                textContent = "@android:color/transparent"
                            })
                            style.appendChild(document.createElement("item").apply {
                                setAttribute("name", "android:windowSplashScreenAnimationDuration")
                                textContent = "0"
                            })
                        }
                    }
                    resources.appendChild(style)
                }
            }
        }
    }
}
