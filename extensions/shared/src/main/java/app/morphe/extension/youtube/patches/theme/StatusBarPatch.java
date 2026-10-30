/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Portions of this file are modified by anddea:
 * Copyright (C) 2026 anddea
 * https://github.com/anddea/revanced-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.extension.youtube.patches.theme;

import android.app.Activity;
import android.app.Application;
import android.content.res.Configuration;
import android.os.Bundle;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.View;
import android.view.WindowInsets;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import app.morphe.extension.youtube.patches.general.NavigationButtonsPatch;
import app.morphe.extension.shared.utils.BaseThemeUtils;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.ResourceType;
import app.morphe.extension.shared.utils.ResourceUtils;
import app.morphe.extension.youtube.patches.theme.ThemePatch.StatusBarTranslucency;
import app.morphe.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public final class StatusBarPatch {
    /**
     * The view the app dims the status bar with. It is empty and the app owns its height and
     * visibility, so only its color is taken over.
     */
    private static final int STATUS_BAR_VIEW = ResourceUtils.getIdentifier(
            ResourceType.ID, "global_status_bar_view");

    /**
     * Injection point. Applies the setting to the overlay itself, including colors that bypass
     * the top-bar fallback hook. Alpha suppresses its drawing without changing system insets,
     * layout space, or the visibility controlled by YouTube's fullscreen handling.
     */
    public static void apply(View bottomBarContainer) {
        StatusBarTranslucency translucency = Settings.STATUS_BAR_TRANSLUCENCY.get();
        if (translucency == StatusBarTranslucency.DEFAULT || STATUS_BAR_VIEW == 0) return;

        try {
            View statusBarView = bottomBarContainer.getRootView().findViewById(STATUS_BAR_VIEW);
            if (statusBarView != null) {
                if (translucency == StatusBarTranslucency.TRANSPARENT) {
                    statusBarView.setAlpha(0);
                } else {
                    // Painted as the foreground, so the dimming the app keeps writing into
                    // the background of this view never shows through again.
                    statusBarView.setForeground(new ColorDrawable(BaseThemeUtils.getAppBackgroundColor()));
                }
            }
        } catch (Exception ex) {
            Logger.printException(() -> "Could not apply status bar translucency", ex);
        }
    }

    /**
     * Injection point.
     * <p>
     * The status bar background view is sized with the stable status bar inset,
     * so when the status bar is hidden it covers the search bar and channel page top bar.
     */
    public static void hideStatusBarBackground(View view) {
        if (!isStatusBarVisible(view.getRootView())) {
            view.setVisibility(View.INVISIBLE);
        } else {
            // Keep the selected translucency when the system status bar is visible.
            apply(view);
        }
    }

    private static boolean isStatusBarVisible(View decorView) {
        WindowInsets insets = decorView.getRootWindowInsets();
        if (insets == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return insets.isVisible(WindowInsets.Type.statusBars());
        }
        //noinspection deprecation
        return (decorView.getSystemUiVisibility() & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0;
    }

    /** Window position from the previous app start, kept stable throughout this session. */
    private static final boolean WINDOW_ABOVE_NAVIGATION_BAR = Settings.WINDOW_ABOVE_NAVIGATION_BAR.get();

    /** The account's edge-to-edge flag is observed without changing this fork's layout. */
    @Nullable
    private static volatile Boolean edgeToEdgeTopBarUsed;

    public static Boolean isEdgeToEdgeTopBar() {
        return edgeToEdgeTopBarUsed;
    }

    public static boolean isWindowAboveNavigationBar() {
        return WINDOW_ABOVE_NAVIGATION_BAR;
    }

    /** Injection point. Observes the flag for the layout fixes without overriding its value. */
    public static boolean useEdgeToEdgeTopBar(boolean original) {
        if (edgeToEdgeTopBarUsed == null) {
            edgeToEdgeTopBarUsed = original;
        }
        return original;
    }

    /**
     * Main activities whose window position is watched once they are started.
     */
    private static final Set<Activity> windowPositionActivities = Collections.newSetFromMap(new WeakHashMap<>());

    /**
     * Callback when an activity is started. The window of the main activity is not created
     * when it is initialized, so its position is watched once it is started.
     */
    private static final Application.ActivityLifecycleCallbacks WINDOW_POSITION_LIFECYCLE_CALLBACKS
            = new Application.ActivityLifecycleCallbacks() {

        public void onActivityStarted(@NonNull Activity activity) {
            try {
                if (windowPositionActivities.remove(activity)
                        && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    watchWindowPosition(activity);
                }
            } catch (Exception ex) {
                Logger.printException(() -> "onActivityStarted window position failure", ex);
            }
        }

        public void onActivityCreated(@NonNull Activity a, @Nullable Bundle b) {}
        public void onActivityResumed(@NonNull Activity a) {}
        public void onActivityPaused(@NonNull Activity a) {}
        public void onActivityStopped(@NonNull Activity a) {}
        public void onActivitySaveInstanceState(@NonNull Activity a, @NonNull Bundle b) {}
        public void onActivityDestroyed(@NonNull Activity a) {}
    };

    private static boolean windowPositionLifecycleCallbacksRegistered;

    /** Injection point. Starts watching the main window when its activity starts. */
    public static void initialize(Activity activity) {
        // The window position is watched once the activity is started, see watchWindowPosition().
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                if (!windowPositionLifecycleCallbacksRegistered) {
                    windowPositionLifecycleCallbacksRegistered = true;
                    activity.getApplication().registerActivityLifecycleCallbacks(WINDOW_POSITION_LIFECYCLE_CALLBACKS);
                }
                windowPositionActivities.add(activity);
            } catch (Exception ex) {
                Logger.printException(() -> "initialize window position failure", ex);
            }
        }
    }

    // This fork copies only the navigation-bar measurement; its status-bar layout stays unchanged.
    /**
     * Some devices keep the window out of the status bar area, for example Samsung devices with
     * the app not allowed to use the camera cutout area. The edge to edge top bar still makes room
     * for the status bar above the top bars, which is then drawn as a second status bar below
     * the real one and covers the top bars (https://github.com/MorpheApp/morphe-patches/issues/2969).
     * <p>
     * The flag is read before the window is laid out, so the position is saved and the flag is
     * turned off from the next app start. Only the full screen window in portrait with the status bar
     * shown is checked, where the status bar is above the window.
     * <p>
     * The same way, a window drawn edge to edge is expected to end behind the system navigation
     * buttons, which the navigation bar covering them relies on. Some devices and Android versions
     * keep it above them, and the room for them is then added twice, see
     * {@link NavigationButtonsPatch#useTranslucentNavigationBar(boolean)}. The bottom of the window is
     * saved, and the flags of the translucent navigation bar are turned off from the next app start.
     */
    @RequiresApi(api = Build.VERSION_CODES.R)
    private static void watchWindowPosition(Activity activity) {
        View decorView = activity.getWindow().getDecorView();
        // Width, height and status bar visibility of the window last checked,
        // so the position is only read when they change.
        final int[] lastSize = {-1, -1, -1};
        final int[] location = new int[2];

        decorView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            try {
                WindowInsets insets = decorView.getRootWindowInsets();
                // The keyboard can resize the window on some devices.
                if (insets == null || insets.isVisible(WindowInsets.Type.ime())
                        || activity.isInMultiWindowMode() || activity.isInPictureInPictureMode()
                        || activity.getResources().getConfiguration().orientation
                        != Configuration.ORIENTATION_PORTRAIT) {
                    return;
                }

                final int width = decorView.getWidth();
                final int height = decorView.getHeight();
                final boolean statusBarVisible = insets.isVisible(WindowInsets.Type.statusBars());
                final int statusBar = statusBarVisible ? 1 : 0;
                if ((width == lastSize[0] && height == lastSize[1] && statusBar == lastSize[2])
                        || width == 0 || height == 0) {
                    return;
                }
                lastSize[0] = width;
                lastSize[1] = height;
                lastSize[2] = statusBar;

                decorView.getLocationOnScreen(location);
                final int windowTop = location[1];
                final int windowBottom = windowTop + height;
                final int displayHeight = activity.getWindowManager().getMaximumWindowMetrics().getBounds().height();
                Logger.printDebug(() -> "Window top: " + windowTop + " bottom: " + windowBottom
                        + " display height: " + displayHeight
                        + " status bar inset: " + insets.getInsets(WindowInsets.Type.statusBars()).top
                        + " stable: " + insets.getInsetsIgnoringVisibility(WindowInsets.Type.statusBars()).top
                        + " cutout: " + insets.getInsets(WindowInsets.Type.displayCutout()).top
                        + " navigation bar inset: " + insets.getInsets(WindowInsets.Type.navigationBars()).bottom);

                // Without the edge to edge top bar the app itself can lay the window out below
                // the status bar (it uses the default display cutout mode on Android 14 and lower)
                // and above the system navigation buttons, so only a window that was drawn edge to edge
                // tells that the device keeps it there. Otherwise the flags would stay off for good
                // after one app start without them.
                final boolean edgeToEdge = Boolean.TRUE.equals(edgeToEdgeTopBarUsed);

                // Only with the system navigation buttons at the bottom of the screen. The navigation bar
                // covering them can change how the window is laid out, so only a window laid out with it
                // tells that the device keeps the window above them.
                if (edgeToEdge && insets.isVisible(WindowInsets.Type.navigationBars())
                        && insets.getInsetsIgnoringVisibility(WindowInsets.Type.navigationBars()).bottom > 0) {
                    final boolean aboveNavigationBar = windowBottom < displayHeight;
                    if ((!aboveNavigationBar || NavigationButtonsPatch.isNavigationBarCoveringSystemButtons())
                            && aboveNavigationBar != Settings.WINDOW_ABOVE_NAVIGATION_BAR.get()) {
                        Logger.printDebug(() -> "Window above navigation bar: " + aboveNavigationBar
                                + ", applied from the next app start");
                        Settings.WINDOW_ABOVE_NAVIGATION_BAR.save(aboveNavigationBar);
                    }
                }
            } catch (Exception ex) {
                Logger.printException(() -> "watchWindowPosition failure", ex);
            }
        });
    }

    private StatusBarPatch() {
    }
}
