package app.revanced.extension.youtube.patches.misc;

import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.shared.ShortsPlayerState;

@SuppressWarnings("unused")
public class BackgroundPlaybackPatch {

    /**
     * Injection point.
     */
    public static boolean isBackgroundPlaybackAllowed(boolean original) {
        if (original) return true;
        return ShortsPlayerState.getCurrent().isClosed();
    }

    /**
     * Injection point.
     */
    public static boolean isBackgroundShortsPlaybackAllowed(boolean original) {
        return !Settings.DISABLE_SHORTS_BACKGROUND_PLAYBACK.get();
    }

}
