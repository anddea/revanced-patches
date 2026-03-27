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
