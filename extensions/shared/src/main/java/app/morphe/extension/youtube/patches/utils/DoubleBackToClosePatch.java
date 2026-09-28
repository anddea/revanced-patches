/*
 * Portions of this file are ported from Morphe:
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.extension.youtube.patches.utils;

import android.app.Activity;

import androidx.annotation.NonNull;

import app.morphe.extension.youtube.settings.Settings;

/**
 * @noinspection ALL
 */
public class DoubleBackToClosePatch {
    /**
     * Time between two back button presses
     */
    private static final long PRESSED_TIMEOUT_MILLISECONDS = Settings.DOUBLE_BACK_TO_CLOSE_TIMEOUT.get();

    private static final Boolean BACK_BUTTON_ALWAYS_EXITS_FEED = Settings.BACK_BUTTON_ALWAYS_EXITS_FEED.get();

    /**
     * Last time back button was pressed
     */
    private static long lastTimeBackPressed = 0;

    /**
     * State whether scroll position reaches the top
     */
    private static boolean isScrollTop = false;

    /**
     * Detect event when back button is pressed
     *
     * @param activity is used when closing the app
     */
    public static void closeActivityOnBackPressed(@NonNull Activity activity) {
        // Check scroll position reaches the top in home feed
        if (!isScrollTop)
            return;

        final long currentTime = System.currentTimeMillis();

        // If the time between two back button presses does not reach PRESSED_TIMEOUT_MILLISECONDS,
        // set lastTimeBackPressed to the current time.
        if (currentTime - lastTimeBackPressed < PRESSED_TIMEOUT_MILLISECONDS ||
                PRESSED_TIMEOUT_MILLISECONDS == 0)
            activity.finish();
        else
            lastTimeBackPressed = currentTime;
    }

    /**
     * Override back button scrolling to the top of the home/subscription feed.
     */
    public static boolean allowBackButtonToScrollToTopOfFeed(boolean original) {
        if (BACK_BUTTON_ALWAYS_EXITS_FEED) {
            return false;
        }
        return original;
    }

    /**
     * Detect event when ScrollView is created by RecyclerView
     * <p>
     * start of ScrollView
     */
    public static void onStartScrollView() {
        isScrollTop = false;
    }

    /**
     * Detect event when the scroll position reaches the top by the back button
     * <p>
     * stop of ScrollView
     */
    public static void onStopScrollView() {
        isScrollTop = true;
    }
}
