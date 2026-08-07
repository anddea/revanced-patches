/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Ported from https://github.com/andronedev/morphe-portal-patch.
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
 * - Nicolas (https://github.com/andronedev)
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

package app.morphe.extension.youtube.dpi

import android.app.Activity
import android.app.Application
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import app.morphe.extension.shared.utils.Logger
import app.morphe.extension.shared.utils.Utils
import app.morphe.extension.youtube.settings.Settings
import java.util.Collections
import java.util.WeakHashMap

@Suppress("DEPRECATION", "unused", "WrongConstant")
object DensityPatch {
    private const val BASELINE_DPI = 160f

    @Volatile
    private var targetDpi = 0

    private val activeActivities = Collections.newSetFromMap(WeakHashMap<Activity, Boolean>())

    @JvmStatic
    fun init(application: Application) {
        try {
            Utils.setContext(application)
            register(application)
        } catch (t: Throwable) {
            Logger.printException({ "init failed" }, t)
        }
    }

    private fun register(application: Application) {
        targetDpi = getSavedDpi()
        Logger.printInfo { "init targetDpi=$targetDpi" }

        if (targetDpi > 0) {
            applyTo(application.resources)
        }

        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {
                activeActivities.add(activity)
                forceDensity(activity)
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                activeActivities.add(activity)
                forceDensity(activity)
            }

            override fun onActivityStarted(activity: Activity) {
                activeActivities.add(activity)
            }

            override fun onActivityResumed(activity: Activity) {
                activeActivities.add(activity)
                forceDensity(activity)
            }

            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {
                activeActivities.remove(activity)
            }
        })

        application.registerComponentCallbacks(object : ComponentCallbacks2 {
            override fun onConfigurationChanged(newConfig: Configuration) {
                if (targetDpi > 0) {
                    applyTo(application.resources)
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onLowMemory() {}
            override fun onTrimMemory(level: Int) {}
        })
    }

    private fun getSavedDpi(): Int {
        try {
            val dpiValue = Settings.CUSTOM_DPI.get()
            if (dpiValue > 0) {
                return dpiValue
            }
        } catch (e: Exception) {
            Logger.printException({ "Failed to read CUSTOM_DPI setting" }, e)
        }
        return 0
    }

    private fun forceDensity(activity: Activity) {
        if (targetDpi <= 0) return
        applyTo(activity.resources)
        val base: Context? = activity.baseContext
        if (base != null && base.resources !== activity.resources) {
            applyTo(base.resources)
        }
    }

    private fun applyTo(resources: Resources) {
        if (targetDpi <= 0) return
        val metrics = resources.displayMetrics
        if (metrics.densityDpi == targetDpi) return

        val scale = targetDpi / BASELINE_DPI
        metrics.densityDpi = targetDpi
        metrics.density = scale
        run { metrics.scaledDensity = scale }

        val configuration = resources.configuration
        configuration.densityDpi = targetDpi

        resources.updateConfiguration(configuration, metrics)
    }
}
