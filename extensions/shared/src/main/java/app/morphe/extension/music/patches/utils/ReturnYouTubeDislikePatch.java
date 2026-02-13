package app.morphe.extension.music.patches.utils;

import static app.morphe.extension.shared.returnyoutubedislike.ReturnYouTubeDislike.Vote;

import android.text.SpannableString;
import android.text.Spanned;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import app.morphe.extension.music.returnyoutubedislike.ReturnYouTubeDislike;
import app.morphe.extension.music.settings.Settings;
import app.morphe.extension.shared.returnyoutubedislike.requests.ReturnYouTubeDislikeApi;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;

/**
 * Handles all interaction of UI patch components.
 * <p>
 * Does not handle creating dislike spans or anything to do with {@link ReturnYouTubeDislikeApi}.
 */
@SuppressWarnings("unused")
public class ReturnYouTubeDislikePatch {
    private static volatile boolean isNewActionBar = false;

    /**
     * Injection point.
     * <p>
     * Called when a litho text component is initially created,
     * and also when a Span is later reused again (such as scrolling off/on screen).
     * <p>
     * This method is sometimes called on the main thread, but it usually is called _off_ the main thread.
     * This method can be called multiple times for the same UI element (including after dislikes was added).
     *
     * @param original Original char sequence was created or reused by Litho.
     * @return The original char sequence (if nothing should change), or a replacement char sequence that contains dislikes.
     */
    public static CharSequence onLithoTextLoaded(@NonNull Object conversionContext,
                                                 @NonNull CharSequence original) {
        try {
            if (!Settings.RYD_ENABLED.get()) {
                return original;
            }

            String conversionContextString = conversionContext.toString();

            if (!conversionContextString.contains("segmented_like_dislike_button.")) {
                return original;
            }
            ReturnYouTubeDislike videoData = currentVideoData;
            if (videoData == null) {
                return original; // User enabled RYD while a video was on screen.
            }
            if (!(original instanceof Spanned)) {
                original = new SpannableString(original);
            }
            return videoData.getDislikesSpan((Spanned) original, true, isNewActionBar);
        } catch (Exception ex) {
            Logger.printException(() -> "onLithoTextLoaded failure", ex);
        }
        return original;
    }

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
            return videoData.getDislikesSpan(original, false, false);
        } catch (Exception ex) {
            Logger.printException(() -> "onSpannedCreated failure", ex);
        }
        return original;
    }

    /**
     * Injection point.
     */
    public static boolean actionBarFeatureFlagLoaded(boolean original) {
        isNewActionBar = original;
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
            if (!Utils.isNetworkConnected()) {
                Logger.printDebug(() -> "Cannot fetch RYD, network is not connected");
                currentVideoData = null;
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
