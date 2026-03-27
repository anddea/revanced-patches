package app.morphe.extension.youtube.patches.player;

import androidx.annotation.NonNull;

import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.shared.PlayerType;
import app.morphe.extension.youtube.utils.VideoUtils;

@SuppressWarnings("unused")
public class EnterFullscreenPatch {
    private static volatile boolean isForeground = true;

    @NonNull
    private static String videoId = "";

    /**
     * Injection point.
     */
    public static void onAppBackgrounded() {
        isForeground = false;
    }

    /**
     * Injection point.
     */
    public static void onAppForegrounded() {
        isForeground = true;
    }

    /**
     * Injection point.
     */
    public static void enterFullscreen(@NonNull String newlyLoadedChannelId, @NonNull String newlyLoadedChannelName,
                                       @NonNull String newlyLoadedVideoId, @NonNull String newlyLoadedVideoTitle,
                                       final long newlyLoadedVideoLength, boolean newlyLoadedLiveStreamValue) {
        try {
            if (!Settings.ENTER_FULLSCREEN.get()) {
                return;
            }
            PlayerType playerType = PlayerType.getCurrent();
            // 1. The user opened the video while playing a video in the feed.
            // 2. This is a valid request, so the videoId is not saved.
            if (playerType == PlayerType.INLINE_MINIMAL) {
                return;
            }
            if (videoId.equals(newlyLoadedVideoId)) {
                return;
            }
            videoId = newlyLoadedVideoId;

            // 1. User clicks home button in [PlayerType.WATCH_WHILE_MAXIMIZED], thus entering audio only mode.
            // 2. PlayerType is still [PlayerType.WATCH_WHILE_MAXIMIZED].
            // 3. Next video starts in audio only mode, then returns to foreground mode.
            // 4. Enters fullscreen for a moment and then returns.
            // We can prevent this by checking if the app is in the foreground.
            if (playerType == PlayerType.WATCH_WHILE_MAXIMIZED && isForeground) {
                // It works without delay, but in this case sometimes portrait videos have landscape orientation.
                Utils.runOnMainThreadDelayed(VideoUtils::enterFullscreenMode, 250L);
            }
        } catch (Exception ex) {
            Logger.printException(() -> "enterFullscreen failure", ex);
        }
    }

}
