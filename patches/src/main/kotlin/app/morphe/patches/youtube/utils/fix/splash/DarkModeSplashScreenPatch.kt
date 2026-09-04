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

package app.morphe.patches.youtube.utils.fix.splash

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.youtube.utils.playservice.is_19_32_or_greater
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import org.w3c.dom.Element

/**
 * Fix the splash screen dark mode background color.
 * In earlier versions of the app this is white and makes no sense for dark mode.
 * This is only required for 19.32 and greater, but is applied to all targets.
 * Only dark mode needs this fix as light mode correctly uses the custom color.
 *
 * This is a bug in unpatched YouTube.
 * Should always be applied even if the `Theme` patch is excluded.
 */
val darkModeSplashScreenPatch = resourcePatch(
    description = "darkModeSplashScreenPatch"
) {
    dependsOn(versionCheckPatch)

    finalize {
        if (!is_19_32_or_greater) {
            return@finalize
        }

        document("res/values-night/styles.xml").use { document ->
            // Create a night mode specific override for the splash screen background.
            val style = document.createElement("style")
            style.setAttribute("name", "Theme.YouTube.Home")
            style.setAttribute("parent", "@style/Base.V27.Theme.YouTube.Home")

            // Fix status and navigation bar showing white on some Android devices,
            // such as SDK 28 Android 10 medium tablet.
            val colorSplashBackgroundColor = "@color/yt_black1"
            arrayOf(
                "android:navigationBarColor" to colorSplashBackgroundColor,
                "android:windowBackground" to colorSplashBackgroundColor,
                "android:colorBackground" to colorSplashBackgroundColor,
                "colorPrimaryDark" to colorSplashBackgroundColor,
                "android:windowLightStatusBar" to "false",
            ).forEach { (name, value) ->
                val styleItem = document.createElement("item")
                styleItem.setAttribute("name", name)
                styleItem.textContent = value
                style.appendChild(styleItem)
            }

            val resourcesNode = document.getElementsByTagName("resources").item(0) as Element
            resourcesNode.appendChild(style)
        }
    }
}
