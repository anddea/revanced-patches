package app.revanced.extension.youtube.patches.video;

import static app.revanced.extension.shared.utils.StringRef.str;

import androidx.annotation.NonNull;

import app.revanced.extension.shared.utils.Utils;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.shared.PlayerType;
import app.revanced.extension.youtube.shared.VideoInformation;

@SuppressWarnings("unused")
public class ReloadVideoPatch {
    private static final long RELOAD_VIDEO_TIME_MILLISECONDS = 15000L;

    @NonNull
    public static String videoId = "";

    /**
     * Injection point.
     */
    public static void newVideoStarted(@NonNull String newlyLoadedChannelId, @NonNull String newlyLoadedChannelName,
                                       @NonNull String newlyLoadedVideoId, @NonNull String newlyLoadedVideoTitle,
                                       final long newlyLoadedVideoLength, boolean newlyLoadedLiveStreamValue) {
        if (!Settings.SKIP_PRELOADED_BUFFER.get())
            return;
        if (PlayerType.getCurrent() == PlayerType.INLINE_MINIMAL)
            return;
        if (videoId.equals(newlyLoadedVideoId))
            return;
        videoId = newlyLoadedVideoId;

        if (newlyLoadedVideoLength < RELOAD_VIDEO_TIME_MILLISECONDS || newlyLoadedLiveStreamValue)
            return;

        final long seekTime = Math.max(RELOAD_VIDEO_TIME_MILLISECONDS, (long) (newlyLoadedVideoLength * 0.5));

        Utils.runOnMainThreadDelayed(() -> reloadVideo(seekTime), 250);
    }

    private static void reloadVideo(final long videoLength) {
        final long lastVideoTime = VideoInformation.getVideoTime();
        final float playbackSpeed = VideoInformation.getPlaybackSpeed();
        final long speedAdjustedTimeThreshold = (long) (playbackSpeed * 300);
        VideoInformation.overrideVideoTime(videoLength);
        VideoInformation.overrideVideoTime(lastVideoTime + speedAdjustedTimeThreshold);

        if (!Settings.SKIP_PRELOADED_BUFFER_TOAST.get())
            return;

        Utils.showToastShort(str("revanced_skipped_preloaded_buffer"));
    }
}
