package app.morphe.extension.youtube.patches.misc;

import app.morphe.extension.shared.settings.BooleanSetting;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.shared.PlayerType;
import app.morphe.extension.youtube.shared.ShortsPlayerState;

@SuppressWarnings("unused")
public class BackgroundPlaybackPatch {
    private static final BooleanSetting DISABLE_SHORTS_BACKGROUND_PLAYBACK =
            Settings.DISABLE_SHORTS_BACKGROUND_PLAYBACK;

    /**
     * Injection point.
     */
    public static boolean isBackgroundPlaybackAllowed(boolean original) {
        if (original) return true;
        return ShortsPlayerState.getCurrent().isClosed() &&
                // 1. Shorts background playback is enabled.
                // 2. Autoplay in feed is turned on.
                // 3. Play Shorts from feed.
                // 4. Media controls appear in status bar.
                // (For unpatched YouTube with Premium accounts, media controls do not appear in the status bar)
                //
                // This is just a visual bug and does not affect Shorts background play in any way.
                // To fix this, just check PlayerType.
                PlayerType.getCurrent() != PlayerType.INLINE_MINIMAL;
    }

    /**
     * Injection point.
     */
    public static boolean isBackgroundShortsPlaybackAllowed(boolean original) {
        return !DISABLE_SHORTS_BACKGROUND_PLAYBACK.get();
    }

}
