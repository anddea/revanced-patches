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

package app.morphe.patches.shared.layout.branding

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.ResourcePatchContext
import app.morphe.util.FilesCompat
import app.morphe.util.ResourceGroup
import app.morphe.util.copyResources
import app.morphe.util.doRecursively
import app.morphe.util.findElementByAttributeValueOrThrow
import app.morphe.util.inputStreamFromBundledResource
import app.morphe.util.inputStreamFromBundledResourceOrThrow
import app.morphe.util.removeFromParent
import app.morphe.util.removeStringsElements
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import java.util.ArrayDeque

// region generated resource contract

internal const val CUSTOM_BRANDING_EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/morphe/extension/shared/patches/CustomBrandingPatch;"
internal const val SPLASHLESS_LAUNCHER_ACTIVITY_CLASS_NAME =
    $$"app.morphe.extension.shared.patches.CustomBrandingPatch$SplashlessLauncherActivity"
private const val RVX_SETTINGS_ICON_FALLBACK_DRAWABLE_CLASS =
    $$"app.morphe.extension.shared.patches.CustomBrandingPatch$RvxSettingsIconFallbackDrawable"

private const val GENERATED_RESOURCE_PREFIX = "morphe_"
private const val CUSTOM_BRANDING_RESOURCE_PREFIX = "${GENERATED_RESOURCE_PREFIX}custom_branding_"
private const val CUSTOM_ICON_KEY = "custom"
private const val CUSTOM_ICON_LABEL = "Custom"
private const val SYSTEM_SPLASH_ALIAS_SUFFIX = "_system"
private const val SYSTEM_SPLASH_THEME_SEPARATOR = "__"
private const val THEME_SPLASH_STYLE_PREFIX = "morphe_theme_splash_"
private const val THEME_SPLASH_NO_ICON_SUFFIX = "_no_icon"

/** Stable icon resource roles shared with the runtime extension. */
private enum class IconResource(private val role: String) {
    ADAPTIVE_BACKGROUND("adaptive_background"),
    ADAPTIVE_FOREGROUND("adaptive_foreground"),
    ADAPTIVE_MONOCHROME("adaptive_monochrome"),
    LAUNCHER("launcher"),
    NOTIFICATION("notification_icon"),
    RVX_SETTINGS("rvx_settings_icon"),
    HEADER("custom_branding_header"),
    SPLASH("custom_branding_splash");

    fun named(iconKey: String) = "$GENERATED_RESOURCE_PREFIX${role}_$iconKey"
}

/** Fixed generated resources read by name from the runtime extension or Android manifest. */
private enum class BrandingResource(key: String) {
    SPLASHLESS_LAUNCHER_STYLE("splashless_launcher"),
    SYSTEM_SPLASH_STYLE("system_splash"),
    ORIGINAL_APP_NAME("original_app_name"),
    CUSTOM_APP_NAME("name_custom"),
    DEFAULT_ICON("default_icon"),
    DEFAULT_NAME_INDEX("default_name_index"),
    MAIN_ACTIVITY("main_activity"),
    ORIGINAL_LAUNCHER("original_launcher");

    val resourceName = "$CUSTOM_BRANDING_RESOURCE_PREFIX$key"
}

// endregion

// region custom resource formats

private val customIconFileExtensions = arrayOf("xml", "png")
private val customIconNamePrefixes = setOf("morphe", "revanced", "rvx")
private val unsupportedAaptInterpolator = Regex(
    """<aapt:attr\s+name="android:interpolator"[^>]*>.*?</aapt:attr>""",
    setOf(RegexOption.DOT_MATCHES_ALL),
)

private val mipmapDirectories = arrayOf(
    "xxxhdpi",
    "xxhdpi",
    "xhdpi",
    "hdpi",
    "mdpi",
)
private val drawableDirectories = mipmapDirectories.map { "drawable-$it" }
private const val SPLASH_LIGHT_TEXT_COLOR = "#000000"
private const val SPLASH_DARK_TEXT_COLOR = "#ffffff"
private val splashWhiteColor = Regex("#ffffff(?![0-9a-fA-F])", RegexOption.IGNORE_CASE)

// endregion

// region models and options

/**
 * A launcher icon that can be exposed by the in-app selector.
 *
 * Stock entries intentionally have no bundled launcher resource. They point at the original
 * launcher, while themed entries are copied to stable resource names at patch time.
 *
 * [hasLauncherResource] is separate from [hasAdaptiveLayers] because a partial custom folder can
 * provide a standalone XML or density-specific PNG launcher. That resource remains usable even
 * when the adaptive background or foreground is missing.
 */
internal data class BrandingIcon(
    val key: String,
    val label: String,
    val hasAdaptiveLayers: Boolean,
    val hasMonochromeLayers: Boolean = hasAdaptiveLayers,
    val hasLauncherResource: Boolean = hasAdaptiveLayers,
)

/** Application-specific details shared by the YouTube and Music branding wrappers. */
internal data class CustomBrandingConfig(
    val resourceRoot: String,
    val adaptiveBackgroundFileName: String,
    val adaptiveForegroundFileName: String,
    val monochromeFileName: String,
    val settingsIconFileName: String,
    val originalSettingsIconKey: String,
    val settingsPreferencePaths: List<String>,
    val settingsPreferenceKey: String,
    val originalLauncherIconName: String,
    val applicationNameKeys: Array<String>,
    val originalNameFallback: String,
    val namePresetLabels: List<String>,
    val icons: List<BrandingIcon>,
    val mainActivityName: String,
    val activityAliasNameWithIntents: String,
    val copyAliasIntentFilters: Boolean,
    val useSplashlessLauncherActivity: Boolean = true,
    /** Source drawable base names used for runtime header replacement. */
    val dynamicHeaderResourceNames: List<String> = emptyList(),
    /** Whether each dynamic header has separate light and dark source files. */
    val dynamicHeaderUsesThemes: Boolean = true,
    /** Source splash logo base name used when an animated-vector is unavailable. */
    val dynamicSplashResourceName: String? = null,
    /** Animated splash icon keys that need separate light and dark text variants. */
    val themedSplashIconKeys: Set<String> = emptySet(),
    /** Drawable directories containing the static splash fallback. */
    val dynamicSplashResourceDirectories: List<String> = drawableDirectories,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CustomBrandingConfig

        if (copyAliasIntentFilters != other.copyAliasIntentFilters) return false
        if (useSplashlessLauncherActivity != other.useSplashlessLauncherActivity) return false
        if (dynamicHeaderUsesThemes != other.dynamicHeaderUsesThemes) return false
        if (resourceRoot != other.resourceRoot) return false
        if (adaptiveBackgroundFileName != other.adaptiveBackgroundFileName) return false
        if (adaptiveForegroundFileName != other.adaptiveForegroundFileName) return false
        if (monochromeFileName != other.monochromeFileName) return false
        if (settingsIconFileName != other.settingsIconFileName) return false
        if (originalSettingsIconKey != other.originalSettingsIconKey) return false
        if (settingsPreferencePaths != other.settingsPreferencePaths) return false
        if (settingsPreferenceKey != other.settingsPreferenceKey) return false
        if (originalLauncherIconName != other.originalLauncherIconName) return false
        if (!applicationNameKeys.contentEquals(other.applicationNameKeys)) return false
        if (originalNameFallback != other.originalNameFallback) return false
        if (namePresetLabels != other.namePresetLabels) return false
        if (icons != other.icons) return false
        if (mainActivityName != other.mainActivityName) return false
        if (activityAliasNameWithIntents != other.activityAliasNameWithIntents) return false
        if (dynamicHeaderResourceNames != other.dynamicHeaderResourceNames) return false
        if (dynamicSplashResourceName != other.dynamicSplashResourceName) return false
        if (themedSplashIconKeys != other.themedSplashIconKeys) return false
        if (dynamicSplashResourceDirectories != other.dynamicSplashResourceDirectories) return false
        return true
    }

    override fun hashCode(): Int {
        var result = copyAliasIntentFilters.hashCode()
        result = 31 * result + useSplashlessLauncherActivity.hashCode()
        result = 31 * result + dynamicHeaderUsesThemes.hashCode()
        result = 31 * result + resourceRoot.hashCode()
        result = 31 * result + adaptiveBackgroundFileName.hashCode()
        result = 31 * result + adaptiveForegroundFileName.hashCode()
        result = 31 * result + monochromeFileName.hashCode()
        result = 31 * result + settingsIconFileName.hashCode()
        result = 31 * result + originalSettingsIconKey.hashCode()
        result = 31 * result + settingsPreferencePaths.hashCode()
        result = 31 * result + settingsPreferenceKey.hashCode()
        result = 31 * result + originalLauncherIconName.hashCode()
        result = 31 * result + applicationNameKeys.contentHashCode()
        result = 31 * result + originalNameFallback.hashCode()
        result = 31 * result + namePresetLabels.hashCode()
        result = 31 * result + icons.hashCode()
        result = 31 * result + mainActivityName.hashCode()
        result = 31 * result + activityAliasNameWithIntents.hashCode()
        result = 31 * result + dynamicHeaderResourceNames.hashCode()
        result = 31 * result + (dynamicSplashResourceName?.hashCode() ?: 0)
        result = 31 * result + themedSplashIconKeys.hashCode()
        result = 31 * result + dynamicSplashResourceDirectories.hashCode()
        return result
    }
}

internal val customBrandingIconOptionDescription = """
    Folder containing custom branding resources. The folder is scanned recursively, so Android
    resource folders can be placed at the root or grouped under folders such as 'launcher',
    'header', 'splash', 'monochrome', and 'settings'.

    Every resource is optional. Icon resources accept XML or PNG files. 
    A launcher icon is also used for the RVX settings entry when no dedicated settings icon 
    is present. A complete 'drawable/avd_anim.xml' splash takes priority over static splash images.
""".trimIndent()

// endregion

// region branding application

/**
 * Applies the common runtime branding resources and manifest aliases.
 *
 * The stock app name remains the fallback application label, while aliases provide every
 * supported app-name and project-icon combination to the in-app selectors.
 */
internal fun ResourcePatchContext.applyCustomBranding(
    config: CustomBrandingConfig,
    customName: String? = null,
    customIconPath: String? = null,
): Boolean {
    require(config.namePresetLabels.size == 4) {
        "Custom branding expects four preset app names."
    }

    copyResources(
        "shared/custombranding",
        ResourceGroup("layout", "morphe_icon_list_item.xml"),
    )

    config.icons.filter { it.hasAdaptiveLayers }.forEach { icon ->
        copyAdaptiveLayers(config, icon)
    }
    val customIcon = copyCustomIcon(config, customIconPath)
    copyDynamicBrandingResources(config)

    var hasRvxSettingsPreference = false

    val originalName = findOriginalAppName(config)
    removeStringsElements(config.applicationNameKeys)
    addBrandingResources(config, originalName, customName, customIcon != null)
    val aliasNameLabels = config.namePresetLabels + (customName ?: CUSTOM_ICON_LABEL)

    config.settingsPreferencePaths.forEach { path ->
        if (!get(path).exists()) return@forEach

        document(path).use { document ->
            hasRvxSettingsPreference =
                document.hasPreference(config.settingsPreferenceKey) || hasRvxSettingsPreference
        }
    }
    if (hasRvxSettingsPreference) copyRvxSettingsIcons(config)

    val systemSplashIconKeys = (config.icons.map { it.key } + CUSTOM_ICON_KEY).distinct()
    var launcherTheme: String? = null
    document("AndroidManifest.xml").use { document ->
        val application = document.getElementsByTagName("application").item(0) as Element
        application.setAttribute(
            "android:label",
            "@string/${BrandingResource.CUSTOM_APP_NAME.resourceName}",
        )
        if (customIcon?.hasLauncherResource == true) {
            // The application icon is not runtime-selectable and is used by Android settings,
            // installers, and some device-specific notification surfaces.
            application.setAttribute(
                "android:icon",
                "@mipmap/${IconResource.LAUNCHER.named(CUSTOM_ICON_KEY)}",
            )
        }

        val source = document.childNodes.findElementByAttributeValueOrThrow(
            "android:name",
            config.activityAliasNameWithIntents,
        )
        val mainActivity = document.childNodes.findElementByAttributeValueOrThrow(
            "android:name",
            config.mainActivityName,
        )
        launcherTheme = mainActivity.getAttribute("android:theme")
            .ifBlank { application.getAttribute("android:theme") }
            .takeIf(String::isNotBlank)
            ?: throw PatchException("Could not find the main activity theme")
        val sourceChildren = List(source.childNodes.length) { source.childNodes.item(it) }

        // This entry must remain a standard activity. Inheriting the host's singleTask launch mode
        // prevents its forwarded MainActivity intent from opening in the current launcher task.
        val splashlessActivity = document.createElement("activity")
        splashlessActivity.setAttribute("android:name", SPLASHLESS_LAUNCHER_ACTIVITY_CLASS_NAME)
        splashlessActivity.setAttribute(
            "android:theme",
            "@style/${BrandingResource.SPLASHLESS_LAUNCHER_STYLE.resourceName}",
        )
        splashlessActivity.setAttribute("android:exported", "false")
        splashlessActivity.setAttribute("android:noHistory", "true")
        application.appendChild(splashlessActivity)

        // Always retain the custom aliases. Removing an alias that was enabled by a previous
        // installation can make the application impossible to launch until it is uninstalled.
        val customAliasIcon = customIcon ?: BrandingIcon(CUSTOM_ICON_KEY, CUSTOM_ICON_LABEL, false)
        val runtimeIcons =
            listOf(BrandingIcon("original", "Stock", false)) + config.icons + customAliasIcon
        val nameCount = aliasNameLabels.size
        val defaultIcon = if (customIcon != null) CUSTOM_ICON_KEY else "original"
        val defaultNameIndex = if (customName != null) nameCount else 1

        fun addAlias(icon: BrandingIcon, nameIndex: Int, useSystemSplash: Boolean) {
            val alias = document.createElement("activity-alias")
            val suffix = if (useSystemSplash) SYSTEM_SPLASH_ALIAS_SUFFIX else ""
            alias.setAttribute("android:name", ".morphe_${icon.key}_$nameIndex$suffix")
            alias.setAttribute(
                "android:enabled",
                (!useSystemSplash && icon.key == defaultIcon && nameIndex == defaultNameIndex).toString(),
            )
            alias.setAttribute("android:exported", "true")
            alias.setAttribute("android:icon", "@mipmap/${iconResourceName(config, icon)}")
            alias.setAttribute(
                "android:label",
                nameResourceName(nameIndex, originalName, aliasNameLabels),
            )
            alias.setAttribute(
                "android:targetActivity",
                if (useSystemSplash || icon.key == "original" || !config.useSplashlessLauncherActivity) {
                    config.mainActivityName
                } else {
                    SPLASHLESS_LAUNCHER_ACTIVITY_CLASS_NAME
                },
            )
            if (config.copyAliasIntentFilters) {
                sourceChildren.forEach { child ->
                    alias.appendChild(child.cloneNode(true))
                }
            } else {
                alias.appendChild(document.createElement("intent-filter").also { filter ->
                    filter.appendChild(document.createElement("action").also { action ->
                        action.setAttribute("android:name", "android.intent.action.MAIN")
                    })
                    filter.appendChild(document.createElement("category").also { category ->
                        category.setAttribute("android:name", "android.intent.category.LAUNCHER")
                    })
                })
            }

            application.appendChild(alias)
        }

        runtimeIcons.forEach { icon ->
            for (nameIndex in 1..nameCount) {
                addAlias(icon, nameIndex, useSystemSplash = false)
                if (icon.key != "original") {
                    addAlias(icon, nameIndex, useSystemSplash = true)
                }
            }
        }

        // The original launcher entry must stop advertising MAIN/LAUNCHER, otherwise Android
        // shows both the original component and the selected alias in the launcher.
        sourceChildren.forEach { child ->
            if (child !is Element || child.tagName != "intent-filter") return@forEach

            val actions = child.getElementsByTagName("action")
            for (index in actions.length - 1 downTo 0) {
                val action = actions.item(index) as? Element ?: continue
                if (action.getAttribute("android:name") == "android.intent.action.MAIN") {
                    action.removeFromParent()
                }
            }
        }
    }
    addLauncherSplashStyles(launcherTheme!!, systemSplashIconKeys)

    return hasRvxSettingsPreference
}

// endregion

// region custom folder resources

/**
 * Copies every recognized resource from a user-provided custom branding folder.
 *
 * Resource folders may appear anywhere below the selected path. Each file is independent: missing
 * adaptive layers fall back to a custom launcher drawable or the stock launcher, while missing
 * headers, splash images, notification icons, and settings icons use their runtime fallbacks.
 * Returning null only hides the custom selector entry when the folder contains no recognized file.
 */
private fun ResourcePatchContext.copyCustomIcon(
    config: CustomBrandingConfig,
    customIconPath: String?,
): BrandingIcon? {
    val path = customIconPath?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val iconPath = File(path)
    if (!iconPath.exists()) {
        throw PatchException("The custom icon path cannot be found: ${iconPath.absolutePath}")
    }
    if (!iconPath.isDirectory) {
        throw PatchException("The custom icon path must be a folder: ${iconPath.absolutePath}")
    }

    val resourceDirectory = get("res")
    val filesByResourceDirectory = iconPath.walkTopDown()
        .filter { file ->
            file.isFile && file.parentFile?.name?.let(::isAndroidResourceDirectory) == true
        }
        .sortedBy(File::getAbsolutePath)
        .groupBy { it.parentFile.name }

    fun findSource(directory: String, vararg names: String): File? {
        val files = filesByResourceDirectory[directory] ?: return null
        names.forEach { name ->
            files.firstOrNull { it.name == name }?.let { return it }
        }
        return null
    }

    fun copyResource(directory: String, targetName: String, vararg sourceNames: String): File? {
        val source = findSource(directory, targetName, *sourceNames) ?: return null
        val targetDirectory = resourceDirectory.resolve(directory).also(File::mkdirs)
        FilesCompat.copy(source, targetDirectory.resolve(targetName))
        return source
    }

    fun iconKeys(vararg resourceNames: String) =
        resourceNames.mapTo(mutableSetOf(), ::normalizedCustomIconKey)

    fun findIconSource(
        directory: String,
        preferredResourceNames: List<String>,
        acceptedKeys: Set<String>,
    ): File? {
        val files = filesByResourceDirectory[directory] ?: return null
        preferredResourceNames.forEach { resourceName ->
            customIconFileExtensions.forEach { extension ->
                files.firstOrNull {
                    it.name.equals("$resourceName.$extension", ignoreCase = true)
                }?.let { return it }
            }
        }
        acceptedKeys.forEach { key ->
            customIconFileExtensions.forEach { extension ->
                files.firstOrNull { source ->
                    source.extension.equals(extension, ignoreCase = true) &&
                        normalizedCustomIconKey(source.nameWithoutExtension) == key
                }?.let { return it }
            }
        }
        return null
    }

    fun copyIconSource(source: File, directory: String, targetResourceName: String): File {
        val sourceExtension = source.extension.lowercase()
        val targetDirectory = resourceDirectory.resolve(directory).also(File::mkdirs)
        customIconFileExtensions.forEach { extension ->
            if (extension != sourceExtension) {
                targetDirectory.resolve("$targetResourceName.$extension").delete()
            }
        }
        FilesCompat.copy(source, targetDirectory.resolve("$targetResourceName.$sourceExtension"))
        return source
    }

    fun copyIconResource(
        directory: String,
        targetResourceName: String,
        preferredResourceNames: List<String>,
        acceptedKeys: Set<String>,
    ): File? {
        val source = findIconSource(directory, preferredResourceNames, acceptedKeys) ?: return null
        return copyIconSource(source, directory, targetResourceName)
    }

    val customAdaptiveBackground = IconResource.ADAPTIVE_BACKGROUND.named(CUSTOM_ICON_KEY)
    val customAdaptiveForeground = IconResource.ADAPTIVE_FOREGROUND.named(CUSTOM_ICON_KEY)
    val customLauncher = IconResource.LAUNCHER.named(CUSTOM_ICON_KEY)
    val customMonochrome = IconResource.ADAPTIVE_MONOCHROME.named(CUSTOM_ICON_KEY)
    val customNotification = IconResource.NOTIFICATION.named(CUSTOM_ICON_KEY)
    val customRvxSettings = IconResource.RVX_SETTINGS.named(CUSTOM_ICON_KEY)
    val customSplash = IconResource.SPLASH.named(CUSTOM_ICON_KEY)

    val customDrawableFiles = filesByResourceDirectory["drawable"].orEmpty()
    val animatedSplash = customDrawableFiles.firstOrNull {
        it.name == "$customSplash.xml"
    } ?: customDrawableFiles.firstOrNull { it.name == "avd_anim.xml" }
    val copiedAnimatedSplash = animatedSplash != null &&
        copyCustomAnimatedVectorSplash(animatedSplash, customDrawableFiles)

    var copiedAny = copiedAnimatedSplash
    var copiedAdaptiveBackground = false
    var copiedAdaptiveForeground = false
    var copiedLauncher = false
    val launcherSources = mutableListOf<Pair<String, File>>()
    val adaptiveBackgroundKeys = iconKeys(
        customAdaptiveBackground,
        config.adaptiveBackgroundFileName,
    ).apply {
        addAll(listOf("adaptive_background", "adaptive_icon_background"))
    }
    val adaptiveForegroundKeys = iconKeys(
        customAdaptiveForeground,
        config.adaptiveForegroundFileName,
    ).apply {
        addAll(listOf("adaptive_foreground", "adaptive_icon_foreground"))
    }
    val launcherKeys = iconKeys(
        customLauncher,
        config.originalLauncherIconName,
    ).apply {
        addAll(listOf("app_icon", "ic_launcher", "ic_launcher_release", "launcher_icon"))
    }
    val mipmapResourceDirectories = filesByResourceDirectory.keys.filter {
        it == "mipmap" || it.startsWith("mipmap-")
    }.sorted()
    mipmapResourceDirectories.forEach { directory ->
        copiedAdaptiveBackground = copyIconResource(
            directory,
            customAdaptiveBackground,
            listOf(customAdaptiveBackground, config.adaptiveBackgroundFileName),
            adaptiveBackgroundKeys,
        ) != null || copiedAdaptiveBackground
        copiedAdaptiveForeground = copyIconResource(
            directory,
            customAdaptiveForeground,
            listOf(customAdaptiveForeground, config.adaptiveForegroundFileName),
            adaptiveForegroundKeys,
        ) != null || copiedAdaptiveForeground
        copyIconResource(
            directory,
            customLauncher,
            listOf(customLauncher, config.originalLauncherIconName),
            launcherKeys,
        )?.let { source ->
            copiedLauncher = true
            launcherSources += directory to source
        }
    }
    copiedAny = copiedAny || copiedAdaptiveBackground || copiedAdaptiveForeground ||
        copiedLauncher

    var hasMonochrome = false
    var copiedNotification = false
    var copiedSettingsIcon = false
    val monochromeSources = mutableListOf<Pair<String, File>>()
    val monochromeKeys = iconKeys(
        customMonochrome,
        config.monochromeFileName,
    ).apply {
        addAll(listOf("adaptive_icon_monochrome", "adaptive_monochrome", "monochrome_icon"))
    }
    val notificationKeys = iconKeys(customNotification).apply {
        addAll(listOf("notification", "notification_icon"))
    }
    val settingsKeys = iconKeys(
        customRvxSettings,
        config.settingsIconFileName,
    ).apply {
        addAll(listOf("settings", "settings_icon", "settings_key", "settings_key_icon"))
    }
    val drawableResourceDirectories = filesByResourceDirectory.keys.filter {
        it == "drawable" || it.startsWith("drawable-")
    }.sorted()
    drawableResourceDirectories.forEach { directory ->
        copyIconResource(
            directory,
            customMonochrome,
            listOf(customMonochrome, config.monochromeFileName),
            monochromeKeys,
        )?.let { source ->
            hasMonochrome = true
            copiedAny = true
            monochromeSources += directory to source
        }
        copiedNotification = copyIconResource(
            directory,
            customNotification,
            listOf(customNotification),
            notificationKeys,
        ) != null || copiedNotification
        copiedSettingsIcon = copyIconResource(
            directory,
            customRvxSettings,
            listOf(customRvxSettings, config.settingsIconFileName),
            settingsKeys,
        ) != null || copiedSettingsIcon

        config.dynamicHeaderResourceNames.forEach { resourceName ->
            if (config.dynamicHeaderUsesThemes) {
                arrayOf("light", "dark").forEach { theme ->
                    val targetName =
                        "${IconResource.HEADER.named("${CUSTOM_ICON_KEY}_${resourceName}_$theme")}.png"
                    copiedAny = copyResource(
                        directory,
                        targetName,
                        "${resourceName}_$theme.png",
                    ) != null || copiedAny
                }
            } else {
                val targetName =
                    "${IconResource.HEADER.named("${CUSTOM_ICON_KEY}_$resourceName")}.png"
                copiedAny = copyResource(
                    directory,
                    targetName,
                    "$resourceName.png",
                ) != null || copiedAny
            }
        }

        if (!copiedAnimatedSplash) {
            config.dynamicSplashResourceName?.let { resourceName ->
                val targetName = "$customSplash.png"
                copiedAny = copyResource(
                    directory,
                    targetName,
                    "$resourceName.png",
                ) != null || copiedAny
            }
        }
    }

    // A monochrome adaptive layer is also a valid notification icon. Use it only when the folder
    // does not provide a notification-specific XML or density-specific PNG.
    if (!copiedNotification) {
        monochromeSources.forEach { (directory, source) ->
            copyIconSource(source, directory, customNotification)
            copiedNotification = true
        }
    }
    copiedAny = copiedAny || copiedNotification

    val hasAdaptiveLayers = copiedAdaptiveBackground && copiedAdaptiveForeground
    val hasExplicitAnyDpiLauncher = launcherSources.any { it.first == "mipmap-anydpi" }
    if (hasAdaptiveLayers && !hasExplicitAnyDpiLauncher) {
        val adaptiveIconDirectory = resourceDirectory.resolve("mipmap-anydpi").also(File::mkdirs)
        val monochromeLayer = if (hasMonochrome) {
            "                <monochrome android:drawable=\"@drawable/$customMonochrome\" />\n"
        } else {
            ""
        }
        adaptiveIconDirectory.resolve("$customLauncher.xml").writeText(
            """<?xml version="1.0" encoding="utf-8"?>
                <adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
                    <background android:drawable="@mipmap/$customAdaptiveBackground" />
                    <foreground android:drawable="@mipmap/$customAdaptiveForeground" />
$monochromeLayer                </adaptive-icon>
            """.trimIndent(),
        )
    }

    // Keep the launcher resource at its normal size for aliases and the manifest. The settings
    // fallback is a separate drawable wrapper so only the RVX settings row gets the smaller icon.
    if (!copiedSettingsIcon && (hasAdaptiveLayers || copiedLauncher)) {
        writeRvxSettingsIconFallback(customRvxSettings)
        copiedSettingsIcon = true
    }
    copiedAny = copiedAny || copiedSettingsIcon

    if (!copiedAny) return null
    return BrandingIcon(
        CUSTOM_ICON_KEY,
        CUSTOM_ICON_LABEL,
        hasAdaptiveLayers,
        hasMonochrome,
        hasAdaptiveLayers || copiedLauncher,
    )
}

private fun isAndroidResourceDirectory(name: String) =
    name == "drawable" || name.startsWith("drawable-") ||
        name == "mipmap" || name.startsWith("mipmap-")

/**
 * Reduces a custom icon resource name to its role key.
 *
 * Project prefixes may be repeated in any order and the optional `custom` suffix is ignored. For
 * example, `rvx_morphe_settings_icon_custom` and `revanced_settings_icon` both become
 * `settings_icon`. Keeping the role key prevents an unrelated icon in the same folder from being
 * selected merely because it uses a recognized project prefix.
 */
private fun normalizedCustomIconKey(resourceName: String): String {
    val parts = resourceName.lowercase().split('_').filter(String::isNotEmpty)
    var startIndex = 0
    while (parts.getOrNull(startIndex)?.let { it in customIconNamePrefixes } == true) startIndex++

    var endIndex = parts.size
    if (parts.getOrNull(endIndex - 1) == CUSTOM_ICON_KEY) endIndex--
    return if (startIndex < endIndex) {
        parts.subList(startIndex, endIndex).joinToString("_")
    } else {
        ""
    }
}

/**
 * Copies a complete custom animated-vector graph to stable names.
 *
 * A missing local companion makes the animation unusable, so the method skips the animation and
 * leaves the stock or static splash in place. External references already present in the decoded
 * app remain unchanged.
 */
private fun ResourcePatchContext.copyCustomAnimatedVectorSplash(
    source: File,
    customDrawableFiles: List<File>,
): Boolean {
    val drawableReference = Regex("""@drawable/([A-Za-z0-9_$]+)""")
    val customResources = customDrawableFiles.associateBy { it.nameWithoutExtension }
    val companionSources = linkedMapOf<String, File>()
    val visitedResources = mutableSetOf<String>()
    val pendingResources = ArrayDeque<String>().apply {
        drawableReference.findAll(source.readText()).forEach { addLast(it.groupValues[1]) }
    }

    fun hasDecodedDrawable(resourceName: String): Boolean =
        get("res").listFiles().orEmpty().asSequence()
            .filter { it.isDirectory && (it.name == "drawable" || it.name.startsWith("drawable-")) }
            .flatMap { it.listFiles().orEmpty().asSequence() }
            .any { it.isFile && it.nameWithoutExtension == resourceName }

    while (pendingResources.isNotEmpty()) {
        val sourceResourceName = pendingResources.removeFirst()
        if (!visitedResources.add(sourceResourceName)) continue

        val companionSource = customResources[sourceResourceName]
        if (companionSource == null) {
            if (!hasDecodedDrawable(sourceResourceName)) return false
            continue
        }

        companionSources[sourceResourceName] = companionSource
        if (companionSource.extension == "xml") {
            drawableReference.findAll(companionSource.readText()).forEach {
                pendingResources.addLast(it.groupValues[1])
            }
        }
    }

    val targetResourceNames = companionSources.keys.mapIndexed { index, sourceResourceName ->
        sourceResourceName to IconResource.SPLASH.named("${CUSTOM_ICON_KEY}_part_$index")
    }.toMap()

    fun rewriteDrawableReferences(sourceXml: String): String {
        var rewritten = unsupportedAaptInterpolator.replace(sourceXml, "")
        targetResourceNames.forEach { (sourceResourceName, targetResourceName) ->
            rewritten = rewritten.replace(
                "@drawable/$sourceResourceName",
                "@drawable/$targetResourceName",
            )
        }
        return rewritten
    }

    val targetDirectory = get("res/drawable").also(File::mkdirs)
    targetDirectory.resolve("${IconResource.SPLASH.named(CUSTOM_ICON_KEY)}.xml")
        .writeText(rewriteDrawableReferences(source.readText()))
    companionSources.forEach { (sourceResourceName, companionSource) ->
        val targetName = targetResourceNames.getValue(sourceResourceName)
        val target = targetDirectory.resolve("$targetName.${companionSource.extension}")
        if (companionSource.extension == "xml") {
            target.writeText(rewriteDrawableReferences(companionSource.readText()))
        } else {
            FilesCompat.copy(companionSource, target)
        }
    }
    return true
}

// endregion

// region bundled branding resources

private fun ResourcePatchContext.copyAdaptiveLayers(
    config: CustomBrandingConfig,
    icon: BrandingIcon,
) {
    val sourceDirectory = "${config.resourceRoot}/branding/${icon.key}/launcher"

    mipmapDirectories.forEach { density ->
        copyBundledResource(
            sourceDirectory,
            "mipmap-$density/${config.adaptiveBackgroundFileName}.png",
            "mipmap-$density/${IconResource.ADAPTIVE_BACKGROUND.named(icon.key)}.png",
        )
        copyBundledResource(
            sourceDirectory,
            "mipmap-$density/${config.adaptiveForegroundFileName}.png",
            "mipmap-$density/${IconResource.ADAPTIVE_FOREGROUND.named(icon.key)}.png",
        )
    }

    if (icon.hasMonochromeLayers) {
        val sourceMonochromeDirectory =
            "${config.resourceRoot}/branding/${icon.key}/monochrome"
        copyBundledResource(
            sourceMonochromeDirectory,
            "drawable/${config.monochromeFileName}.xml",
            "drawable/${IconResource.ADAPTIVE_MONOCHROME.named(icon.key)}.xml",
        )
        copyBundledResource(
            sourceMonochromeDirectory,
            "drawable/${config.monochromeFileName}.xml",
            "drawable/${IconResource.NOTIFICATION.named(icon.key)}.xml",
        )
    }

    val adaptiveIconDirectory = get("res/mipmap-anydpi")
    if (!adaptiveIconDirectory.exists()) adaptiveIconDirectory.mkdirs()
    val monochromeLayer = if (icon.hasMonochromeLayers) {
        "                <monochrome android:drawable=\"@drawable/${
            IconResource.ADAPTIVE_MONOCHROME.named(icon.key)
        }\" />\n"
    } else {
        ""
    }

    adaptiveIconDirectory.resolve("${IconResource.LAUNCHER.named(icon.key)}.xml").writeText(
        """<?xml version="1.0" encoding="utf-8"?>
            <adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
                <background android:drawable="@mipmap/${IconResource.ADAPTIVE_BACKGROUND.named(icon.key)}" />
                <foreground android:drawable="@mipmap/${IconResource.ADAPTIVE_FOREGROUND.named(icon.key)}" />
$monochromeLayer            </adaptive-icon>
        """.trimIndent(),
    )
}

/** Copies icon-specific header and splash resources to stable names consumed by the extension. */
private fun ResourcePatchContext.copyDynamicBrandingResources(config: CustomBrandingConfig) {
    val dynamicHeaderIcons = config.icons.filter {
        it.hasAdaptiveLayers && it.key != config.originalSettingsIconKey
    }

    dynamicHeaderIcons.forEach { icon ->
        val brandingDirectory = "${config.resourceRoot}/branding/${icon.key}"
        config.dynamicHeaderResourceNames.forEach { resourceName ->
            drawableDirectories.forEach { directory ->
                if (config.dynamicHeaderUsesThemes) {
                    copyBundledResource(
                        "$brandingDirectory/header",
                        "$directory/${resourceName}_light.png",
                        "$directory/${IconResource.HEADER.named("${icon.key}_${resourceName}_light")}.png",
                    )
                    copyBundledResource(
                        "$brandingDirectory/header",
                        "$directory/${resourceName}_dark.png",
                        "$directory/${IconResource.HEADER.named("${icon.key}_${resourceName}_dark")}.png",
                    )
                } else {
                    copyBundledResource(
                        "$brandingDirectory/header",
                        "$directory/$resourceName.png",
                        "$directory/${IconResource.HEADER.named("${icon.key}_$resourceName")}.png",
                    )
                }
            }
        }
    }

    val splashResourceName = config.dynamicSplashResourceName ?: return
    config.icons.forEach { icon ->
        val brandingDirectory = "${config.resourceRoot}/branding/${icon.key}"

        val hasAnimatedVector = runCatching {
            copyAnimatedVectorSplash(
                brandingDirectory,
                icon.key,
                icon.key in config.themedSplashIconKeys,
            )
        }.isSuccess

        if (!hasAnimatedVector) {
            config.dynamicSplashResourceDirectories.forEach { directory ->
                copyBundledResource(
                    "$brandingDirectory/splash",
                    "$directory/$splashResourceName.png",
                    "$directory/${IconResource.SPLASH.named(icon.key)}.png",
                )
            }
        }
    }
}

/**
 * Copies an animated-vector splash and its generated companion drawables.
 *
 * Some bundled vectors reference names such as {@code $avd_anim__0}, whose animator XML can then
 * reference additional resources such as {@code $$avd_anim__1__0}. Those names are shared by
 * multiple source icons, so traverse the complete graph and rewrite every local reference to a
 * stable, icon-specific resource name.
 */
private fun ResourcePatchContext.copyAnimatedVectorSplash(
    brandingDirectory: String,
    iconKey: String,
    useThemeVariants: Boolean,
) {
    val sourceDirectory = "$brandingDirectory/splash"
    val source = inputStreamFromBundledResourceOrThrow(
        sourceDirectory,
        "drawable/avd_anim.xml",
    ).use { it.reader().readText() }
    val drawableReference = Regex("""@drawable/([A-Za-z0-9_$]+)""")
    val companionSources = linkedMapOf<String, String>()
    val visitedResources = mutableSetOf<String>()
    val pendingResources = ArrayDeque<String>().apply {
        drawableReference.findAll(source).forEach { addLast(it.groupValues[1]) }
    }

    while (pendingResources.isNotEmpty()) {
        val sourceResourceName = pendingResources.removeFirst()
        if (!visitedResources.add(sourceResourceName)) continue

        val companionSource = runCatching {
            inputStreamFromBundledResourceOrThrow(
                sourceDirectory,
                "drawable/$sourceResourceName.xml",
            ).use { it.reader().readText() }
        }.getOrNull() ?: continue

        companionSources[sourceResourceName] = companionSource
        drawableReference.findAll(companionSource).forEach {
            pendingResources.addLast(it.groupValues[1])
        }
    }

    // The patcher's aapt macro processor supports inline drawable, animation, and fillColor
    // resources, but not inline interpolators. Dropping only that optional wrapper keeps the
    // animated vector valid and lets Android use its default interpolator.
    fun rewriteDrawableReferences(
        sourceXml: String,
        targetResourceNames: Map<String, String>,
        textColor: String? = null,
    ): String {
        var rewritten = unsupportedAaptInterpolator.replace(sourceXml, "")
        targetResourceNames.forEach { (sourceResourceName, targetResourceName) ->
            rewritten = rewritten.replace(
                "@drawable/$sourceResourceName",
                "@drawable/$targetResourceName",
            )
        }
        textColor?.let { rewritten = splashWhiteColor.replace(rewritten, it) }
        return rewritten
    }

    fun writeSplashVariant(suffix: String, textColor: String?) {
        val variantResourceNames = companionSources.keys.mapIndexed { index, sourceResourceName ->
            sourceResourceName to IconResource.SPLASH.named("${iconKey}${suffix}_part_$index")
        }.toMap()
        val target = get("res/drawable/${IconResource.SPLASH.named("$iconKey$suffix")}.xml")
        target.parentFile?.mkdirs()
        target.writeText(rewriteDrawableReferences(source, variantResourceNames, textColor))

        companionSources.forEach { (sourceResourceName, companionSource) ->
            get("res/drawable/${variantResourceNames.getValue(sourceResourceName)}.xml")
                .writeText(
                    rewriteDrawableReferences(companionSource, variantResourceNames, textColor),
                )
        }
    }

    writeSplashVariant("", null)
    if (useThemeVariants) {
        writeSplashVariant("_light", SPLASH_LIGHT_TEXT_COLOR)
        writeSplashVariant("_dark", SPLASH_DARK_TEXT_COLOR)
    }
}

// endregion

// region launcher and settings integration

/**
 * Adds the two launcher handoff paths used by custom branding.
 *
 * Normal aliases clear Android 12's starting-window artwork and forward to the main activity,
 * where the custom animation's size and timing remain controllable. System-splash aliases launch
 * the main activity directly. Since an activity alias cannot override its target activity's theme,
 * the runtime extension persists the stable generated system-splash style before the next launch.
 * The original theme remains the parent to preserve all app- and version-specific window
 * attributes.
 */
private fun ResourcePatchContext.addLauncherSplashStyles(
    parent: String,
    systemSplashIconKeys: List<String>,
) {
    val styleName = BrandingResource.SPLASHLESS_LAUNCHER_STYLE.resourceName
    ensureValuesFile("values", "styles.xml")
    ensureValuesFile("values-v31", "styles.xml")

    document("res/values/styles.xml").use { document ->
        val style = addStyle(document.documentElement, styleName, parent)

        // Android 11 and earlier have no platform splash API. The forwarding activity is only a
        // transparent host for the custom AVD, so do not let its inherited starting window show
        // before the main activity attaches the animation in its first frame.
        arrayOf(
            "android:windowDisablePreview" to "true",
            "android:windowIsTranslucent" to "true",
            "android:windowBackground" to "@android:color/transparent",
            "android:windowAnimationStyle" to "@null",
            "android:backgroundDimEnabled" to "false",
        ).forEach { (name, value) ->
            style.appendChild(document.createElement("item").also {
                it.setAttribute("name", name)
                it.textContent = value
            })
        }
    }

    document("res/values-v31/styles.xml").use { document ->
        val style = addStyle(document.documentElement, styleName, parent)
        style.appendChild(document.createElement("item").also {
            it.setAttribute("name", "android:windowDisablePreview")
            it.textContent = "true"
        })
        // Clear every artwork layer because OEM splash implementations may retain the branding
        // image or icon background even when the primary animated icon is transparent.
        arrayOf(
            "android:windowSplashScreenAnimatedIcon",
            "android:windowSplashScreenBrandingImage",
            "android:windowSplashScreenIconBackgroundColor",
        ).forEach { attribute ->
            style.appendChild(document.createElement("item").also {
                it.setAttribute("name", attribute)
                it.textContent = "@android:color/transparent"
            })
        }
        style.appendChild(document.createElement("item").also {
            it.setAttribute("name", "android:windowSplashScreenAnimationDuration")
            it.textContent = "0"
        })
    }

    document("res/values/styles.xml").use { document ->
        systemSplashIconKeys.forEach { iconKey ->
            addStyle(document.documentElement, systemSplashStyleName(iconKey), parent)
        }
    }

    document("res/values-v31/styles.xml").use { document ->
        systemSplashIconKeys.forEach { iconKey ->
            val hasSplashResource = hasSplashResource(iconKey)
            val style = addStyle(document.documentElement, systemSplashStyleName(iconKey), parent)
            style.appendChild(document.createElement("item").also {
                it.setAttribute("name", "android:windowSplashScreenAnimatedIcon")
                it.textContent = if (hasSplashResource) {
                    "@drawable/${IconResource.SPLASH.named(iconKey)}"
                } else {
                    "@android:color/transparent"
                }
            })
            style.appendChild(document.createElement("item").also {
                it.setAttribute("name", "android:windowSplashScreenBrandingImage")
                it.textContent = "@android:color/transparent"
            })
            style.appendChild(document.createElement("item").also {
                it.setAttribute("name", "android:windowSplashScreenIconBackgroundColor")
                it.textContent = "@android:color/transparent"
            })
            style.appendChild(document.createElement("item").also {
                it.setAttribute("name", "android:windowSplashScreenAnimationDuration")
                it.textContent = if (hasSplashResource) "1000" else "0"
            })
            if (hasSplashResource) {
                style.appendChild(document.createElement("item").also {
                    it.setAttribute("name", "android:windowSplashScreenBehavior")
                    it.textContent = "icon_preferred"
                })
            }
        }
    }
}

private fun systemSplashStyleName(iconKey: String) =
    "${BrandingResource.SYSTEM_SPLASH_STYLE.resourceName}_$iconKey"

/**
 * Combines Theme's concrete preset backgrounds with branding's generated splash animations.
 *
 * Android resolves the starting-window style before the app process can install its runtime color
 * overlay. A child style for every available preset keeps that background process-safe while
 * inheriting the selected branding icon attributes on Android 12 and later. This is finalized
 * after all resource patches execute so Custom branding remains usable without forcing Theme as a
 * dependency.
 */
internal fun ResourcePatchContext.addCustomBrandingSystemSplashThemeStyles() {
    val stylesV31 = get("res/values-v31/styles.xml")
    if (!stylesV31.exists()) return

    val systemSplashStylePrefix =
        "${BrandingResource.SYSTEM_SPLASH_STYLE.resourceName}_"
    val systemSplashStyleNames = document("res/values-v31/styles.xml").use { document ->
        val resources = document.documentElement
        (0 until resources.childNodes.length)
            .map { resources.childNodes.item(it) }
            .filterIsInstance<Element>()
            .filter { element ->
                element.tagName == "style" &&
                    element.getAttribute("name").startsWith(systemSplashStylePrefix) &&
                    SYSTEM_SPLASH_THEME_SEPARATOR !in element.getAttribute("name")
            }
            .map { it.getAttribute("name") }
    }
    if (systemSplashStyleNames.isEmpty()) return

    val themeSplashStyleNames = document("res/values-v31/styles.xml").use { document ->
        val resources = document.documentElement
        (0 until resources.childNodes.length)
            .map { resources.childNodes.item(it) }
            .filterIsInstance<Element>()
            .filter { element ->
                val name = element.getAttribute("name")
                element.tagName == "style" &&
                    name.startsWith(THEME_SPLASH_STYLE_PREFIX) &&
                    !name.endsWith(THEME_SPLASH_NO_ICON_SUFFIX)
            }
            .map { it.getAttribute("name") }
    }
    if (themeSplashStyleNames.isEmpty()) return

    listOf("values", "values-v31").forEach { valuesDirectory ->
        val path = "res/$valuesDirectory/styles.xml"
        if (!get(path).exists()) return@forEach

        document(path).use { document ->
            val resources = document.documentElement
            val sourceSystemStyles = (0 until resources.childNodes.length)
                .map { resources.childNodes.item(it) }
                .filterIsInstance<Element>()
                .filter { it.tagName == "style" }
                .associateBy { it.getAttribute("name") }

            systemSplashStyleNames.forEach { systemSplashStyleName ->
                val sourceSystemStyle = sourceSystemStyles[systemSplashStyleName]
                themeSplashStyleNames.forEach { themeSplashStyleName ->
                    val themeKey = themeSplashStyleName.removePrefix(THEME_SPLASH_STYLE_PREFIX)
                    val combinedStyleName = systemSplashStyleName +
                        SYSTEM_SPLASH_THEME_SEPARATOR + themeKey
                    val style = addStyle(
                        resources,
                        combinedStyleName,
                        "@style/$themeSplashStyleName",
                    )

                    // The base values style only needs the preset parent. API 31+ additionally
                    // overrides its splash artwork with the selected branding animation.
                    if (valuesDirectory == "values-v31") {
                        sourceSystemStyle?.childNodes?.let { childNodes ->
                            for (index in 0 until childNodes.length) {
                                style.appendChild(childNodes.item(index).cloneNode(true))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun ResourcePatchContext.hasSplashResource(iconKey: String): Boolean =
    get("res").walkTopDown().any { file ->
        file.isFile && file.nameWithoutExtension == IconResource.SPLASH.named(iconKey)
    }

/** Copies an optional dedicated settings icon, otherwise uses that preset's launcher at runtime. */
private fun ResourcePatchContext.copyRvxSettingsIcons(config: CustomBrandingConfig) {
    fun copy(sourceIconKey: String, targetIconKey: String) {
        val sourceDirectory = "${config.resourceRoot}/branding/$sourceIconKey/settings"
        val sourceResource = "drawable/${config.settingsIconFileName}.xml"
        val targetResourceName = IconResource.RVX_SETTINGS.named(targetIconKey)
        val source = inputStreamFromBundledResource(sourceDirectory, sourceResource)
        if (source == null) {
            writeRvxSettingsIconFallback(targetResourceName)
            return
        }

        val target = get("res/drawable/$targetResourceName.xml")
        target.parentFile?.mkdirs()
        source.use { FilesCompat.copy(it, target) }
    }

    copy(config.originalSettingsIconKey, "original")
    config.icons.forEach { icon -> copy(icon.key, icon.key) }
}

/** Writes the runtime wrapper used when a launcher is the only available branding icon. */
private fun ResourcePatchContext.writeRvxSettingsIconFallback(resourceName: String) {
    val target = get("res/drawable/$resourceName.xml")
    target.parentFile?.mkdirs()
    target.writeText(
        """<?xml version="1.0" encoding="utf-8"?>
            <drawable xmlns:android="http://schemas.android.com/apk/res/android"
                class="$RVX_SETTINGS_ICON_FALLBACK_DRAWABLE_CLASS" />
        """.trimIndent(),
    )
}

/**
 * Preserves the currently selected static Music settings icon as a fallback, then replaces its
 * public resource with a drawable that resolves the in-app branding choice at inflation time.
 */
internal fun ResourcePatchContext.installDynamicRvxSettingsIcon(settingsIconFileName: String) {
    val source = get("res/drawable/$settingsIconFileName.xml")
    if (!source.exists()) return
    val drawableClass =
        $$"app.morphe.extension.shared.patches.CustomBrandingPatch$RvxSettingsIconDrawable"
    if (source.readText().contains(drawableClass)) return

    source.copyTo(
        get("res/drawable/morphe_rvx_settings_icon_fallback.xml"),
        overwrite = true,
    )
    source.writeText(
        """<?xml version="1.0" encoding="utf-8"?>
            <drawable xmlns:android="http://schemas.android.com/apk/res/android"
                class="$drawableClass" />
        """.trimIndent(),
    )
}

/** Installs the Cairo RVX row used by both dynamic branding and static visual-icon fallbacks. */
internal fun ResourcePatchContext.installYouTubeRvxSettingsIconLayout() {
    val source = get("res/drawable/revanced_settings_key_icon.xml")
    if (!source.exists()) return

    source.copyTo(
        get("res/drawable/revanced_settings_cairo_key_icon.xml"),
        overwrite = true,
    )
    document("res/drawable/revanced_settings_cairo_key_icon.xml").use { document ->
        document.doRecursively loop@{ node ->
            if (node !is Element || node.tagName != "group") return@loop
            node.setAttribute("android:scaleX", "0.6")
            node.setAttribute("android:scaleY", "0.6")
        }
    }

    get("res/drawable/revanced_circle_mask.xml").writeText(
        """<?xml version="1.0" encoding="utf-8"?>
            <shape xmlns:android="http://schemas.android.com/apk/res/android"
                android:shape="oval">
                <solid android:color="#00000000" />
            </shape>
        """.trimIndent(),
    )
    get("res/layout/revanced_preference_with_icon.xml").writeText(
        $$"""<?xml version="1.0" encoding="utf-8"?>
            <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
                android:orientation="horizontal"
                android:layout_width="fill_parent"
                android:layout_height="wrap_content"
                android:minHeight="54.0dp">
                <view
                    class="app.morphe.extension.youtube.patches.general.FixPreferenceIconPatch$RvxSettingsIconView"
                    android:id="@+id/revanced_custom_icon"
                    android:layout_width="48dp"
                    android:layout_height="48dp"
                    android:layout_gravity="center|end"
                    android:layout_marginHorizontal="6.0dp"
                    android:contentDescription="@null"
                    android:scaleType="fitCenter"
                    android:background="@drawable/revanced_circle_mask"
                    android:clipToOutline="true"
                    android:src="@drawable/revanced_settings_cairo_key_icon" />
                <TextView
                    android:id="@android:id/title"
                    android:layout_width="fill_parent"
                    android:layout_height="wrap_content"
                    android:layout_gravity="center_vertical"
                    android:gravity="center"
                    android:textAlignment="viewStart"
                    android:textColor="?attr/ytTextPrimary"
                    android:textSize="@dimen/medium_font_size" />
            </LinearLayout>
        """.trimIndent(),
    )

    val cairoSettings = get("res/xml/settings_fragment_cairo.xml")
    if (!cairoSettings.exists()) return
    document("res/xml/settings_fragment_cairo.xml").use { document ->
        document.doRecursively loop@{ node ->
            if (node !is Element) return@loop
            val key = node.getAttribute("android:key").removePrefix("@string/")
            if (key == "revanced_settings_key") {
                node.setAttribute("android:icon", "@drawable/revanced_settings_cairo_key_icon")
                node.setAttribute("android:layout", "@layout/revanced_preference_with_icon")
            }
        }
    }
}

// endregion

// region XML resource helpers

private fun ResourcePatchContext.copyBundledResource(
    sourceDirectory: String,
    sourceResource: String,
    targetResource: String,
) {
    val target = get("res").resolve(targetResource)
    target.parentFile?.mkdirs()
    FilesCompat.copy(
        inputStreamFromBundledResourceOrThrow(sourceDirectory, sourceResource),
        target,
    )
}

/** Finds the RVX entry without replacing YouTube's version-specific preference implementation. */
private fun Document.hasPreference(preferenceKey: String): Boolean =
    getElementsByTagName("*").let { nodes ->
        (0 until nodes.length).any { index ->
            val element = nodes.item(index) as Element
            // YouTube uses both literal keys and string-resource references across versions.
            element.getAttribute("android:key").removePrefix("@string/") == preferenceKey
        }
    }

private fun ResourcePatchContext.findOriginalAppName(config: CustomBrandingConfig): String {
    val valuesFile = get("res/values/strings.xml")
    if (valuesFile.exists()) {
        document("res/values/strings.xml").use { document ->
            val strings = document.getElementsByTagName("string")
            for (index in 0 until strings.length) {
                val element = strings.item(index) as? Element ?: continue
                if (element.getAttribute("name") in config.applicationNameKeys) {
                    val value = element.textContent
                    if (!value.isNullOrBlank() && !value.startsWith("@string/")) {
                        return value
                    }
                }
            }
        }
    }
    return config.originalNameFallback
}

private fun ResourcePatchContext.addBrandingResources(
    config: CustomBrandingConfig,
    originalName: String,
    customName: String?,
    hasCustomIcon: Boolean,
) {
    val nameLabels = config.namePresetLabels + listOfNotNull(customName?.let { CUSTOM_ICON_LABEL })
    val iconEntries = listOf(BrandingIcon("original", "Stock", false)) + config.icons +
        if (hasCustomIcon) listOf(BrandingIcon(CUSTOM_ICON_KEY, CUSTOM_ICON_LABEL, true)) else emptyList()

    ensureValuesFile("strings.xml")
    ensureValuesFile("arrays.xml")

    document("res/values/strings.xml").use { document ->
        val resources = document.documentElement

        addString(resources, BrandingResource.ORIGINAL_APP_NAME.resourceName, originalName)
        // Keep the generated defaults aligned with SharedYouTubeSettings. A value equal to a
        // Setting's default is intentionally removed from SharedPreferences, so these defaults
        // must not be a different, patch-time-only value or the next launch would undo the user's
        // selection.
        addString(
            resources,
            BrandingResource.CUSTOM_APP_NAME.resourceName,
            customName ?: originalName,
        )
        addString(
            resources,
            BrandingResource.DEFAULT_ICON.resourceName,
            if (hasCustomIcon) CUSTOM_ICON_KEY else "original",
        )
        addString(
            resources,
            BrandingResource.DEFAULT_NAME_INDEX.resourceName,
            if (customName != null) (config.namePresetLabels.size + 1).toString() else "1",
        )
        addString(resources, BrandingResource.MAIN_ACTIVITY.resourceName, config.mainActivityName)
        addString(
            resources,
            BrandingResource.ORIGINAL_LAUNCHER.resourceName,
            config.originalLauncherIconName,
        )

        config.applicationNameKeys.forEach { key ->
            addString(resources, key, originalName)
        }

    }

    document("res/values/arrays.xml").use { document ->
        val resources = document.documentElement
        addStringArray(
            resources,
            "morphe_custom_branding_name_entries",
            nameLabels,
        )
        addStringArray(
            resources,
            "morphe_custom_branding_name_entry_values",
            nameLabels.indices.map { (it + 1).toString() },
        )
        addStringArray(
            resources,
            "morphe_custom_branding_icon_entries",
            iconEntries.map { it.label },
        )
        addStringArray(
            resources,
            "morphe_custom_branding_icon_entry_values",
            iconEntries.map { it.key },
        )
    }
}

private fun ResourcePatchContext.ensureValuesFile(directory: String, fileName: String) {
    val file = get("res/$directory/$fileName")
    if (!file.exists()) {
        file.parentFile?.mkdirs()
        file.writeText("<?xml version=\"1.0\" encoding=\"utf-8\"?><resources />")
    }
}

private fun ResourcePatchContext.ensureValuesFile(fileName: String) =
    ensureValuesFile("values", fileName)

@Suppress("SameParameterValue")
private fun addStyle(
    resources: Element,
    name: String,
    parent: String,
): Element {
    removeResource(resources, "style", name)
    return resources.appendChild(resources.ownerDocument.createElement("style").also { style ->
        style.setAttribute("name", name)
        style.setAttribute("parent", parent)
    }) as Element
}

private fun addString(resources: Element, name: String, value: String) {
    removeResource(resources, "string", name)
    resources.appendChild(resources.ownerDocument.createElement("string").also { element ->
        element.setAttribute("name", name)
        element.textContent = value
    })
}

private fun addStringArray(resources: Element, name: String, values: List<String>) {
    removeResource(resources, "string-array", name)
    resources.appendChild(resources.ownerDocument.createElement("string-array").also { array ->
        array.setAttribute("name", name)
        values.forEach { value ->
            array.appendChild(resources.ownerDocument.createElement("item").also { item ->
                item.textContent = value
            })
        }
    })
}

private fun removeResource(resources: Element, tagName: String, name: String) {
    for (index in resources.childNodes.length - 1 downTo 0) {
        val element = resources.childNodes.item(index) as? Element ?: continue
        if (element.tagName == tagName && element.getAttribute("name") == name) {
            resources.removeChild(element)
        }
    }
}

private fun iconResourceName(
    config: CustomBrandingConfig,
    icon: BrandingIcon,
) = if (icon.hasLauncherResource) {
    IconResource.LAUNCHER.named(icon.key)
} else {
    config.originalLauncherIconName
}

private fun nameResourceName(index: Int, originalName: String, labels: List<String>) =
    if (index == 1) originalName else labels[index - 1]

// endregion
