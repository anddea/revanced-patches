package app.morphe.extension.youtube.patches.misc;

import app.morphe.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public class DebuggingPatch {

    public static boolean enableWatchNextProcessingDelay() {
        return Settings.ENABLE_WATCH_NEXT_PROCESSING_DELAY.get();
    }

    public static int getWatchNextProcessingDelay(int original) {
        return Settings.ENABLE_WATCH_NEXT_PROCESSING_DELAY.get()
                ? Settings.WATCH_NEXT_PROCESSING_DELAY.get()
                : original;
    }
}
