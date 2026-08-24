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
import app.morphe.util.inputStreamFromBundledResourceOrThrow
import app.morphe.util.removeFromParent
import app.morphe.util.removeStringsElements
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import java.util.ArrayDeque

internal const val CUSTOM_BRANDING_EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/morphe/extension/shared/patches/CustomBrandingPatch;"
internal const val SPLASHLESS_LAUNCHER_ACTIVITY_CLASS_NAME =
    $$"app.morphe.extension.shared.patches.CustomBrandingPatch$SplashlessLauncherActivity"

private const val ADAPTIVE_BACKGROUND_PREFIX = "morphe_adaptive_background_"
private const val ADAPTIVE_FOREGROUND_PREFIX = "morphe_adaptive_foreground_"
private const val ADAPTIVE_MONOCHROME_PREFIX = "morphe_adaptive_monochrome_"
private const val LAUNCHER_PREFIX = "morphe_launcher_"
private const val NOTIFICATION_ICON_PREFIX = "morphe_notification_icon_"
private const val RVX_SETTINGS_ICON_PREFIX = "morphe_rvx_settings_icon_"
private const val HEADER_PREFIX = "morphe_custom_branding_header_"
private const val SPLASH_PREFIX = "morphe_custom_branding_splash_"
private const val SPLASHLESS_LAUNCHER_STYLE = "morphe_custom_branding_splashless_launcher"
private const val ORIGINAL_APP_NAME_RESOURCE = "morphe_custom_branding_original_app_name"
private const val CUSTOM_APP_NAME_RESOURCE = "morphe_custom_branding_name_custom"
private const val DEFAULT_ICON_RESOURCE = "morphe_custom_branding_default_icon"
private const val DEFAULT_NAME_INDEX_RESOURCE = "morphe_custom_branding_default_name_index"
private const val MAIN_ACTIVITY_RESOURCE = "morphe_custom_branding_main_activity"
private const val ORIGINAL_LAUNCHER_RESOURCE = "morphe_custom_branding_original_launcher"
private const val CUSTOM_ICON_KEY = "custom"
private const val CUSTOM_ICON_LABEL = "Custom"
private val customAdaptiveFileNames = arrayOf(
    "${ADAPTIVE_BACKGROUND_PREFIX}${CUSTOM_ICON_KEY}.png",
    "${ADAPTIVE_FOREGROUND_PREFIX}${CUSTOM_ICON_KEY}.png",
)
private const val CUSTOM_LAUNCHER_FILE_NAME = "${LAUNCHER_PREFIX}${CUSTOM_ICON_KEY}.png"
private const val CUSTOM_MONOCHROME_RESOURCE_NAME =
    "${ADAPTIVE_MONOCHROME_PREFIX}${CUSTOM_ICON_KEY}"
private const val CUSTOM_MONOCHROME_FILE_NAME =
    "$CUSTOM_MONOCHROME_RESOURCE_NAME.xml"
private const val CUSTOM_NOTIFICATION_ICON_FILE_NAME =
    "${NOTIFICATION_ICON_PREFIX}${CUSTOM_ICON_KEY}.xml"
private const val CUSTOM_RVX_SETTINGS_ICON_FILE_NAME =
    "${RVX_SETTINGS_ICON_PREFIX}${CUSTOM_ICON_KEY}.xml"
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

/**
 * A launcher icon that can be exposed by the in-app selector.
 *
 * Stock entries intentionally have no bundled launcher resource. They point at the original
 * launcher, while themed entries are copied to stable resource names at patch time.
 *
 * [hasLauncherResource] is separate from [hasAdaptiveLayers] because a partial custom folder can
 * provide only legacy density-specific launcher images. Those images remain usable even when the
 * adaptive background or foreground is missing.
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
        result = 31 * result + dynamicSplashResourceDirectories.hashCode()
        return result
    }
}

internal val customBrandingIconOptionDescription = """
    Folder containing custom branding resources. The folder is scanned recursively, so Android
    resource folders can be placed at the root or grouped under folders such as 'launcher',
    'header', 'splash', 'monochrome', and 'settings'.

    Every resource is optional. Original app resource names and generated '*_custom' names are
    both accepted. Missing files keep the stock or bundled resource for that part of the branding.
    A complete 'drawable/avd_anim.xml' splash takes priority over static splash images.
""".trimIndent()

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

    var launcherTheme: String? = null
    document("AndroidManifest.xml").use { document ->
        val application = document.getElementsByTagName("application").item(0) as Element
        application.setAttribute("android:label", "@string/$CUSTOM_APP_NAME_RESOURCE")
        if (customIcon?.hasLauncherResource == true) {
            // The application icon is not runtime-selectable and is used by Android settings,
            // installers, and some device-specific notification surfaces.
            application.setAttribute("android:icon", "@mipmap/$LAUNCHER_PREFIX$CUSTOM_ICON_KEY")
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
        splashlessActivity.setAttribute("android:theme", "@style/$SPLASHLESS_LAUNCHER_STYLE")
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

        runtimeIcons.forEach { icon ->
            for (nameIndex in 1..nameCount) {
                val alias = document.createElement("activity-alias")
                alias.setAttribute("android:name", ".morphe_${icon.key}_$nameIndex")
                alias.setAttribute(
                    "android:enabled",
                    (icon.key == defaultIcon && nameIndex == defaultNameIndex).toString(),
                )
                alias.setAttribute("android:exported", "true")
                alias.setAttribute("android:icon", "@mipmap/${iconResourceName(config, icon)}")
                alias.setAttribute(
                    "android:label",
                    nameResourceName(nameIndex, originalName, aliasNameLabels),
                )
                alias.setAttribute(
                    "android:targetActivity",
                    if (icon.key == "original" || !config.useSplashlessLauncherActivity) {
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
    addSplashlessLauncherStyle(launcherTheme!!)

    return hasRvxSettingsPreference
}

/**
 * Copies every recognized resource from a user-provided custom branding folder.
 *
 * Resource folders may appear anywhere below the selected path. Each file is independent: missing
 * adaptive layers fall back to legacy custom launcher images or the stock launcher, while missing
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

    val customDrawableFiles = filesByResourceDirectory["drawable"].orEmpty()
    val animatedSplash = customDrawableFiles.firstOrNull {
        it.name == "${SPLASH_PREFIX}${CUSTOM_ICON_KEY}.xml"
    } ?: customDrawableFiles.firstOrNull { it.name == "avd_anim.xml" }
    val copiedAnimatedSplash = animatedSplash != null &&
        copyCustomAnimatedVectorSplash(animatedSplash, customDrawableFiles)

    var copiedAny = copiedAnimatedSplash
    var copiedAdaptiveBackground = false
    var copiedAdaptiveForeground = false
    var copiedLegacyLauncher = false
    val mipmapResourceDirectories = filesByResourceDirectory.keys.filter {
        it == "mipmap" || it.startsWith("mipmap-")
    }
    mipmapResourceDirectories.forEach { directory ->
        copiedAdaptiveBackground = copyResource(
            directory,
            customAdaptiveFileNames[0],
            "${config.adaptiveBackgroundFileName}.png",
        ) != null || copiedAdaptiveBackground
        copiedAdaptiveForeground = copyResource(
            directory,
            customAdaptiveFileNames[1],
            "${config.adaptiveForegroundFileName}.png",
        ) != null || copiedAdaptiveForeground
        if (!directory.startsWith("mipmap-anydpi")) {
            copiedLegacyLauncher = copyResource(
                directory,
                CUSTOM_LAUNCHER_FILE_NAME,
                "${config.originalLauncherIconName}.png",
            ) != null || copiedLegacyLauncher
        }
    }
    copiedAny = copiedAny || copiedAdaptiveBackground || copiedAdaptiveForeground ||
        copiedLegacyLauncher

    var hasMonochrome = false
    var copiedNotification = false
    val monochromeSources = mutableListOf<Pair<String, File>>()
    val drawableResourceDirectories = filesByResourceDirectory.keys.filter {
        it == "drawable" || it.startsWith("drawable-")
    }
    drawableResourceDirectories.forEach { directory ->
        copyResource(
            directory,
            CUSTOM_MONOCHROME_FILE_NAME,
            "${config.monochromeFileName}.xml",
        )?.let { source ->
            hasMonochrome = true
            copiedAny = true
            monochromeSources += directory to source
        }
        val copiedNotificationXml = copyResource(
            directory,
            CUSTOM_NOTIFICATION_ICON_FILE_NAME,
        ) != null
        copiedNotification = copiedNotificationXml || copiedNotification
        if (!copiedNotificationXml) {
            copiedNotification = copyResource(
                directory,
                CUSTOM_NOTIFICATION_ICON_FILE_NAME.replaceAfterLast('.', "png"),
            ) != null || copiedNotification
        }
        copiedAny = copyResource(
            directory,
            CUSTOM_RVX_SETTINGS_ICON_FILE_NAME,
            "${config.settingsIconFileName}.xml",
        ) != null || copiedAny

        config.dynamicHeaderResourceNames.forEach { resourceName ->
            if (config.dynamicHeaderUsesThemes) {
                arrayOf("light", "dark").forEach { theme ->
                    val targetName =
                        "${HEADER_PREFIX}${CUSTOM_ICON_KEY}_${resourceName}_$theme.png"
                    copiedAny = copyResource(
                        directory,
                        targetName,
                        "${resourceName}_$theme.png",
                    ) != null || copiedAny
                }
            } else {
                val targetName = "${HEADER_PREFIX}${CUSTOM_ICON_KEY}_$resourceName.png"
                copiedAny = copyResource(
                    directory,
                    targetName,
                    "$resourceName.png",
                ) != null || copiedAny
            }
        }

        if (!copiedAnimatedSplash) {
            config.dynamicSplashResourceName?.let { resourceName ->
                val targetName = "${SPLASH_PREFIX}${CUSTOM_ICON_KEY}.png"
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
            val targetDirectory = resourceDirectory.resolve(directory).also(File::mkdirs)
            FilesCompat.copy(source, targetDirectory.resolve(CUSTOM_NOTIFICATION_ICON_FILE_NAME))
            copiedNotification = true
        }
    }
    copiedAny = copiedAny || copiedNotification

    val hasAdaptiveLayers = copiedAdaptiveBackground && copiedAdaptiveForeground
    if (hasAdaptiveLayers) {
        val adaptiveIconDirectory = resourceDirectory.resolve("mipmap-anydpi").also(File::mkdirs)
        val monochromeLayer = if (hasMonochrome) {
            "                <monochrome android:drawable=\"@drawable/$CUSTOM_MONOCHROME_RESOURCE_NAME\" />\n"
        } else {
            ""
        }
        adaptiveIconDirectory.resolve("$LAUNCHER_PREFIX$CUSTOM_ICON_KEY.xml").writeText(
            """<?xml version="1.0" encoding="utf-8"?>
                <adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
                    <background android:drawable="@mipmap/${ADAPTIVE_BACKGROUND_PREFIX}${CUSTOM_ICON_KEY}" />
                    <foreground android:drawable="@mipmap/${ADAPTIVE_FOREGROUND_PREFIX}${CUSTOM_ICON_KEY}" />
$monochromeLayer                </adaptive-icon>
            """.trimIndent(),
        )
    }

    if (!copiedAny) return null
    return BrandingIcon(
        CUSTOM_ICON_KEY,
        CUSTOM_ICON_LABEL,
        hasAdaptiveLayers,
        hasMonochrome,
        hasAdaptiveLayers || copiedLegacyLauncher,
    )
}

private fun isAndroidResourceDirectory(name: String) =
    name == "drawable" || name.startsWith("drawable-") ||
        name == "mipmap" || name.startsWith("mipmap-")

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
        sourceResourceName to "${SPLASH_PREFIX}${CUSTOM_ICON_KEY}_part_$index"
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
    targetDirectory.resolve("${SPLASH_PREFIX}${CUSTOM_ICON_KEY}.xml")
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

private fun ResourcePatchContext.copyAdaptiveLayers(
    config: CustomBrandingConfig,
    icon: BrandingIcon,
) {
    val sourceDirectory = "${config.resourceRoot}/branding/${icon.key}/launcher"

    mipmapDirectories.forEach { density ->
        copyBundledResource(
            sourceDirectory,
            "mipmap-$density/${config.adaptiveBackgroundFileName}.png",
            "mipmap-$density/${ADAPTIVE_BACKGROUND_PREFIX}${icon.key}.png",
        )
        copyBundledResource(
            sourceDirectory,
            "mipmap-$density/${config.adaptiveForegroundFileName}.png",
            "mipmap-$density/${ADAPTIVE_FOREGROUND_PREFIX}${icon.key}.png",
        )
    }

    if (icon.hasMonochromeLayers) {
        val sourceMonochromeDirectory =
            "${config.resourceRoot}/branding/${icon.key}/monochrome"
        copyBundledResource(
            sourceMonochromeDirectory,
            "drawable/${config.monochromeFileName}.xml",
            "drawable/${ADAPTIVE_MONOCHROME_PREFIX}${icon.key}.xml",
        )
        copyBundledResource(
            sourceMonochromeDirectory,
            "drawable/${config.monochromeFileName}.xml",
            "drawable/${NOTIFICATION_ICON_PREFIX}${icon.key}.xml",
        )
    }

    val adaptiveIconDirectory = get("res/mipmap-anydpi")
    if (!adaptiveIconDirectory.exists()) adaptiveIconDirectory.mkdirs()
    val monochromeLayer = if (icon.hasMonochromeLayers) {
        "                <monochrome android:drawable=\"@drawable/${ADAPTIVE_MONOCHROME_PREFIX}${icon.key}\" />\n"
    } else {
        ""
    }

    adaptiveIconDirectory.resolve("$LAUNCHER_PREFIX${icon.key}.xml").writeText(
        """<?xml version="1.0" encoding="utf-8"?>
            <adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
                <background android:drawable="@mipmap/${ADAPTIVE_BACKGROUND_PREFIX}${icon.key}" />
                <foreground android:drawable="@mipmap/${ADAPTIVE_FOREGROUND_PREFIX}${icon.key}" />
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
                        "$directory/${HEADER_PREFIX}${icon.key}_${resourceName}_light.png",
                    )
                    copyBundledResource(
                        "$brandingDirectory/header",
                        "$directory/${resourceName}_dark.png",
                        "$directory/${HEADER_PREFIX}${icon.key}_${resourceName}_dark.png",
                    )
                } else {
                    copyBundledResource(
                        "$brandingDirectory/header",
                        "$directory/$resourceName.png",
                        "$directory/${HEADER_PREFIX}${icon.key}_$resourceName.png",
                    )
                }
            }
        }
    }

    val splashResourceName = config.dynamicSplashResourceName ?: return
    config.icons.forEach { icon ->
        val brandingDirectory = "${config.resourceRoot}/branding/${icon.key}"

        val hasAnimatedVector = runCatching {
            copyAnimatedVectorSplash(brandingDirectory, icon.key)
        }.isSuccess

        if (!hasAnimatedVector) {
            config.dynamicSplashResourceDirectories.forEach { directory ->
                copyBundledResource(
                    "$brandingDirectory/splash",
                    "$directory/$splashResourceName.png",
                    "$directory/${SPLASH_PREFIX}${icon.key}.png",
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

    val targetResourceNames = companionSources.keys.mapIndexed { index, sourceResourceName ->
        sourceResourceName to "${SPLASH_PREFIX}${iconKey}_part_$index"
    }.toMap()

    // The patcher's aapt macro processor supports inline drawable, animation, and fillColor
    // resources, but not inline interpolators. Dropping only that optional wrapper keeps the
    // animated vector valid and lets Android use its default interpolator.
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

    val target = get("res/drawable/${SPLASH_PREFIX}${iconKey}.xml")
    target.parentFile?.mkdirs()
    target.writeText(rewriteDrawableReferences(source))

    companionSources.forEach { (sourceResourceName, companionSource) ->
        get("res/drawable/${targetResourceNames.getValue(sourceResourceName)}.xml")
            .writeText(rewriteDrawableReferences(companionSource))
    }
}

/**
 * Removes Android's automatic launcher-icon preview before the in-app splash animation.
 *
 * Launcher aliases inherit the target activity theme, so the wrapper must be installed on the
 * target activity rather than on each alias. The original theme remains the parent to preserve all
 * app- and version-specific window attributes. Android ignores {@code windowDisablePreview} for
 * launches from system surfaces on recent versions, but it does not create a starting window for a
 * translucent activity. The wrapper never draws UI and finishes in {@code onCreate}, so making its
 * window transparent removes the system splash without exposing an intermediate screen.
 */
private fun ResourcePatchContext.addSplashlessLauncherStyle(parent: String) {
    ensureValuesFile("values", "styles.xml")
    ensureValuesFile("values-v31", "styles.xml")

    fun addWindowItems(style: Element) {
        arrayOf(
            "android:windowDisablePreview" to "true",
            "android:windowIsTranslucent" to "true",
            "android:windowBackground" to "@android:color/transparent",
            "android:windowAnimationStyle" to "@null",
            "android:backgroundDimEnabled" to "false",
        ).forEach { (name, value) ->
            style.appendChild(style.ownerDocument.createElement("item").also {
                it.setAttribute("name", name)
                it.textContent = value
            })
        }
    }

    document("res/values/styles.xml").use { document ->
        addWindowItems(addStyle(document.documentElement, SPLASHLESS_LAUNCHER_STYLE, parent))
    }

    document("res/values-v31/styles.xml").use { document ->
        val style = addStyle(document.documentElement, SPLASHLESS_LAUNCHER_STYLE, parent)
        addWindowItems(style)
        style.appendChild(document.createElement("item").also {
            it.setAttribute("name", "android:windowSplashScreenAnimatedIcon")
            it.textContent = "@android:color/transparent"
        })
        style.appendChild(document.createElement("item").also {
            it.setAttribute("name", "android:windowSplashScreenAnimationDuration")
            it.textContent = "0"
        })
    }
}

private fun ResourcePatchContext.copyRvxSettingsIcons(config: CustomBrandingConfig) {
    copyBundledResource(
        "${config.resourceRoot}/branding/${config.originalSettingsIconKey}/settings",
        "drawable/${config.settingsIconFileName}.xml",
        "drawable/${RVX_SETTINGS_ICON_PREFIX}original.xml",
    )

    config.icons.forEach { icon ->
        copyBundledResource(
            "${config.resourceRoot}/branding/${icon.key}/settings",
            "drawable/${config.settingsIconFileName}.xml",
            "drawable/${RVX_SETTINGS_ICON_PREFIX}${icon.key}.xml",
        )
    }
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

        addString(resources, ORIGINAL_APP_NAME_RESOURCE, originalName)
        // Keep the generated defaults aligned with SharedYouTubeSettings. A value equal to a
        // Setting's default is intentionally removed from SharedPreferences, so these defaults
        // must not be a different, patch-time-only value or the next launch would undo the user's
        // selection.
        addString(resources, CUSTOM_APP_NAME_RESOURCE, customName ?: originalName)
        addString(resources, DEFAULT_ICON_RESOURCE, if (hasCustomIcon) CUSTOM_ICON_KEY else "original")
        addString(
            resources,
            DEFAULT_NAME_INDEX_RESOURCE,
            if (customName != null) (config.namePresetLabels.size + 1).toString() else "1",
        )
        addString(resources, MAIN_ACTIVITY_RESOURCE, config.mainActivityName)
        addString(resources, ORIGINAL_LAUNCHER_RESOURCE, config.originalLauncherIconName)

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
    "$LAUNCHER_PREFIX${icon.key}"
} else {
    config.originalLauncherIconName
}

private fun nameResourceName(index: Int, originalName: String, labels: List<String>) =
    if (index == 1) originalName else labels[index - 1]
