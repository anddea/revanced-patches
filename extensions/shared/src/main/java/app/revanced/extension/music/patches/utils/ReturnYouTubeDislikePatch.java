package app.revanced.extension.music.patches.utils;

import static app.revanced.extension.shared.returnyoutubedislike.ReturnYouTubeDislike.Vote;

import android.text.Spanned;

import androidx.annotation.Nullable;

import app.revanced.extension.music.returnyoutubedislike.ReturnYouTubeDislike;
import app.revanced.extension.music.settings.Settings;
import app.revanced.extension.shared.returnyoutubedislike.requests.ReturnYouTubeDislikeApi;
import app.revanced.extension.shared.utils.Logger;

/**
 * Handles all interaction of UI patch components.
 * <p>
 * Does not handle creating dislike spans or anything to do with {@link ReturnYouTubeDislikeApi}.
 */
@SuppressWarnings("unused")
public class ReturnYouTubeDislikePatch {
    /**
     * RYD data for the current video on screen.
     */
    @Nullable
    private static volatile ReturnYouTubeDislike currentVideoData;

    public static void onRYDStatusChange(boolean rydEnabled) {
        ReturnYouTubeDislikeApi.resetRateLimits();
        // Must remove all values to protect against using stale data
        // if the user enables RYD while a video is on screen.
        clearData();
    }

    private static void clearData() {
        currentVideoData = null;
    }

    /**
     * Injection point
     * <p>
     * Called when a Shorts dislike Spannable is created
     */
    public static Spanned onSpannedCreated(Spanned original) {
        try {
            if (original == null) {
                return null;
            }
            ReturnYouTubeDislike videoData = currentVideoData;
            if (videoData == null) {
                return original; // User enabled RYD while a video was on screen.
            }
            return videoData.getDislikesSpan(original);
        } catch (Exception ex) {
            Logger.printException(() -> "onSpannedCreated failure", ex);
        }
        return original;
    }

    /**
     * Injection point.
     */
    public static void newVideoLoaded(@Nullable String videoId) {
        try {
            if (!Settings.RYD_ENABLED.get()) {
                return;
            }
            if (videoId == null || videoId.isEmpty()) {
                return;
            }
            if (videoIdIsSame(currentVideoData, videoId)) {
                return;
            }
            currentVideoData = ReturnYouTubeDislike.getFetchForVideoId(videoId);
        } catch (Exception ex) {
            Logger.printException(() -> "newVideoLoaded failure", ex);
        }
    }

    private static boolean videoIdIsSame(@Nullable ReturnYouTubeDislike fetch, @Nullable String videoId) {
        return (fetch == null && videoId == null)
                || (fetch != null && fetch.getVideoId().equals(videoId));
    }

    /**
     * Injection point.
     * <p>
     * Called when the user likes or dislikes.
     */
    public static void sendVote(int vote) {
        try {
            if (!Settings.RYD_ENABLED.get()) {
                return;
            }
            ReturnYouTubeDislike videoData = currentVideoData;
            if (videoData == null) {
                Logger.printDebug(() -> "Cannot send vote, as current video data is null");
                return; // User enabled RYD while a regular video was minimized.
            }

            for (Vote v : Vote.values()) {
                if (v.value == vote) {
                    videoData.sendVote(v);

                    return;
                }
            }
            Logger.printException(() -> "Unknown vote type: " + vote);
        } catch (Exception ex) {
            Logger.printException(() -> "sendVote failure", ex);
        }
    }
}
