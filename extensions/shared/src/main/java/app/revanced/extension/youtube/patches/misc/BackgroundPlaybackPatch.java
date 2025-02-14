package app.revanced.extension.youtube.patches.misc;

import app.revanced.extension.shared.settings.BooleanSetting;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.shared.PlayerType;
import app.revanced.extension.youtube.shared.ShortsPlayerState;

@SuppressWarnings("unused")
public class BackgroundPlaybackPatch {
    private static final BooleanSetting DISABLE_SHORTS_BACKGROUND_PLAYBACK =
            Settings.DISABLE_SHORTS_BACKGROUND_PLAYBACK;

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
        return !DISABLE_SHORTS_BACKGROUND_PLAYBACK.get();
    }

}
