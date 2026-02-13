package app.morphe.extension.youtube.patches.spoof;

import app.morphe.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public class ReloadVideoPatch {
    private static final boolean SPOOF_STREAMING_DATA_RELOAD_VIDEO_BUTTON =
            Settings.SPOOF_STREAMING_DATA.get() &&
                    Settings.SPOOF_STREAMING_DATA_RELOAD_VIDEO_BUTTON.get();
    private static final boolean SPOOF_STREAMING_DATA_RELOAD_VIDEO_BUTTON_ALWAYS_SHOW =
            Settings.SPOOF_STREAMING_DATA_RELOAD_VIDEO_BUTTON_ALWAYS_SHOW.get();

    private static volatile int progressBarVisibility = 0;

    /**
     * Injection point.
     * Hooks the visibility of the loading circle (progress bar) that appears when buffering occurs.
     */
    public static void setProgressBarVisibility(int visibility) {
        if (SPOOF_STREAMING_DATA_RELOAD_VIDEO_BUTTON &&
                !SPOOF_STREAMING_DATA_RELOAD_VIDEO_BUTTON_ALWAYS_SHOW) {
            progressBarVisibility = visibility;
        }
    }

    public static boolean isProgressBarVisible() {
        return progressBarVisibility == 0;
    }
}
