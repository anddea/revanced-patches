/*
 * Portions of this file are ported from Morphe:
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/2518
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.shared.layout.branding

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.ResourcePatchContext
import org.w3c.dom.Element
import java.io.File

/**
 * Captures stock icon references before branding rewrites the manifest.
 *
 * The installer type is not reported to patches, so it is inferred from the package name, which
 * only a regular install changes. Other patches change it while executing, so this is accurate
 * only after all patches have executed.
 */
internal fun ResourcePatchContext.prepareMountedBranding(
    originalLauncherIconName: String,
    originalNotificationIconName: String,
): ResourcePatchContext.() -> Unit {
    val originalAppPackageName = packageMetadata.packageName
    val iconReferences = mutableSetOf("mipmap" to originalLauncherIconName)

    document("AndroidManifest.xml").use { document ->
        val application = document.getElementsByTagName("application").item(0) as Element

        // Some launchers show the round icon, and it can point to a different resource.
        arrayOf("android:icon", "android:roundIcon").forEach { attributeName ->
            val reference = application.getAttribute(attributeName)
            val typeIndex = reference.indexOf('/')

            // Framework resources such as '@android:mipmap/sym_def_app_icon' cannot be replaced.
            if (!reference.startsWith("@") || typeIndex < 0 || reference.contains(':')) {
                return@forEach
            }

            iconReferences += reference.substring(1, typeIndex) to reference.substring(typeIndex + 1)
        }

        // A launcher activity can override the application icon in the stock manifest.
        arrayOf("activity", "activity-alias").forEach { tagName ->
            val components = application.getElementsByTagName(tagName)
            for (index in 0 until components.length) {
                val component = components.item(index) as Element
                val categories = component.getElementsByTagName("category")
                if ((0 until categories.length).none {
                        (categories.item(it) as Element).getAttribute("android:name") ==
                            "android.intent.category.LAUNCHER"
                    }) continue
                val reference = component.getAttribute("android:icon")
                val typeIndex = reference.indexOf('/')
                if (reference.startsWith("@") && typeIndex >= 0 && !reference.contains(':')) {
                    iconReferences += reference.substring(1, typeIndex) to reference.substring(typeIndex + 1)
                }
            }
        }
    }

    return mountedBranding@{
        val isMounted = document("AndroidManifest.xml").use {
            it.documentElement.getAttribute("package") == originalAppPackageName
        }
        if (!isMounted) return@mountedBranding

        val iconStyle = document("res/values/strings.xml").use { document ->
            val strings = document.getElementsByTagName("string")
            (0 until strings.length).map { strings.item(it) as Element }
                .first { it.getAttribute("name") == "morphe_custom_branding_default_icon" }
                .textContent
        }
        if (iconStyle == "original") return@mountedBranding

        // A mounted install keeps the original app installed and the system parses the
        // manifest of the stock APK, so the launch aliases and any manifest branding
        // attribute are never used. Resource ids are unchanged by patching and are still
        // resolved from the mounted APK, and replacing what those ids point to is the only
        // way the branding of a mounted install can be changed.
        val launcherName = "morphe_launcher_$iconStyle"
        val launcherFiles = resourceDirectories("mipmap").flatMap { directory ->
            directory.listFiles { file ->
                file.isFile && file.nameWithoutExtension == launcherName
            }.orEmpty().asList()
        }
        // Stock presets have no generated launcher and keep the original resources.
        if (launcherFiles.isEmpty()) return@mountedBranding
        val launcherXml = resourceDirectories("mipmap")
            .map { it.resolve("$launcherName.xml") }
            .firstOrNull { it.exists() }
            ?.readBytes()
            ?: """<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
                <item android:drawable="@mipmap/$launcherName" />
            </layer-list>""".trimIndent().toByteArray()
        var replacedIcons = 0

        iconReferences.forEach { (resourceType, resourceName) ->
            resourceDirectories(resourceType).forEach directory@{ typeDirectory ->
                // Only the adaptive icon is replaced. A raster icon of the same name is used by
                // Android 7 and older, which the target apps no longer support.
                val iconFile = typeDirectory.resolve("$resourceName.xml")
                if (!iconFile.exists()) return@directory

                iconFile.writeBytes(launcherXml)
                replacedIcons++
            }
        }

        if (replacedIcons == 0) {
            throw PatchException(
                "Could not find an adaptive launcher icon to replace: " +
                    iconReferences.joinToString { (type, name) -> "@$type/$name" }
            )
        }

        val notificationName = "morphe_notification_icon_$iconStyle"
        val notificationIcons = resourceDirectories("drawable").flatMap { directory ->
            directory.listFiles { file ->
                file.isFile && file.nameWithoutExtension == notificationName
            }.orEmpty().asList()
        }
        if (notificationIcons.isEmpty()) return@mountedBranding

        // The original icons are removed and not overwritten, because they are raster images for each
        // density and a density qualified resource is a better match than the vector that replaces them.
        val originalIcons = resourceDirectories("drawable").flatMap { directory ->
            directory.listFiles { file ->
                file.isFile && file.nameWithoutExtension == originalNotificationIconName
            }.orEmpty().asList()
        }
        if (originalIcons.isEmpty()) return@mountedBranding
        originalIcons.forEach { file ->
            if (!file.delete()) throw PatchException("Could not remove notification icon: $file")
        }
        notificationIcons.forEach { sourceFile ->
            sourceFile.copyTo(
                target = sourceFile.parentFile.resolve("$originalNotificationIconName.${sourceFile.extension}"),
                overwrite = true,
            )
        }
    }
}

/**
 * The resource directories of a type, such as 'drawable' and every 'drawable-<qualifier>'.
 */
private fun ResourcePatchContext.resourceDirectories(resourceType: String): List<File> {
    val directories = get("res").listFiles { file ->
        file.isDirectory && (file.name == resourceType || file.name.startsWith("$resourceType-"))
    } ?: throw PatchException("Could not find the app resources")

    return directories.asList()
}
