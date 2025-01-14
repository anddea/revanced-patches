package app.revanced.extension.youtube.patches.swipe;

import android.view.View;

import java.lang.ref.WeakReference;

import app.revanced.extension.youtube.settings.Settings;

@SuppressWarnings({"unused", "deprecation"})
public class SwipeControlsPatch {
    private static WeakReference<View> fullscreenEngagementOverlayViewRef = new WeakReference<>(null);

    /**
     * Injection point.
     */
    public static boolean disableHDRAutoBrightness() {
        return Settings.DISABLE_HDR_AUTO_BRIGHTNESS.get();
    }

    /**
     * Injection point.
     */
    public static boolean disableSwipeToSwitchVideo() {
        return !Settings.DISABLE_SWIPE_TO_SWITCH_VIDEO.get();
    }

    /**
     * Injection point.
     */
    public static boolean disableSwipeToEnterFullscreenModeBelowThePlayer() {
        return !Settings.DISABLE_SWIPE_TO_ENTER_FULLSCREEN_MODE_BELOW_THE_PLAYER.get();
    }

    /**
     * Injection point.
     */
    public static boolean disableSwipeToEnterFullscreenModeInThePlayer(boolean original) {
        return !Settings.DISABLE_SWIPE_TO_ENTER_FULLSCREEN_MODE_IN_THE_PLAYER.get() && original;
    }

    /**
     * Injection point.
     */
    public static boolean disableSwipeToExitFullscreenMode(boolean original) {
        return !Settings.DISABLE_SWIPE_TO_EXIT_FULLSCREEN_MODE.get() && original;
    }

    /**
     * Injection point.
     *
     * @param fullscreenEngagementOverlayView R.layout.fullscreen_engagement_overlay
     */
    public static void setFullscreenEngagementOverlayView(View fullscreenEngagementOverlayView) {
        fullscreenEngagementOverlayViewRef = new WeakReference<>(fullscreenEngagementOverlayView);
    }

    public static boolean isEngagementOverlayVisible() {
        final View engagementOverlayView = fullscreenEngagementOverlayViewRef.get();
        return engagementOverlayView != null && engagementOverlayView.getVisibility() == View.VISIBLE;
    }

}
