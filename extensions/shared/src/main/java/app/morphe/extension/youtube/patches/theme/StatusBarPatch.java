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

import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.View;
import android.view.WindowInsets;

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

    private StatusBarPatch() {
    }
}
