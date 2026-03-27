package app.morphe.extension.music.shared;

import static app.morphe.extension.shared.utils.Utils.getFormattedTimeStamp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;

/**
 * Hooking class for the current playing video.
 */
@SuppressWarnings("unused")
public final class VideoInformation {
    private static final float DEFAULT_YOUTUBE_MUSIC_PLAYBACK_SPEED = 1.0f;
    /**
     * Prefix present in all Sample player parameters signature.
     */
    private static final String SAMPLES_PLAYER_PARAMETERS = "8AEB";

    @NonNull
    private static String videoId = "";

    private static long videoLength = 0;
    private static long videoTime = -1;

    @NonNull
    private static volatile String playerResponseVideoId = "";
    private static volatile boolean playerResponseVideoIdIsSample;

    /**
     * The current playback speed
     */
    private static float playbackSpeed = DEFAULT_YOUTUBE_MUSIC_PLAYBACK_SPEED;

    /**
     * Injection point.
     */
    public static void initialize() {
        videoTime = -1;
        videoLength = 0;
        Logger.printDebug(() -> "Initialized Player");
    }

    /**
     * Injection point.
     */
    public static void initializeMdx() {
        Logger.printDebug(() -> "Initialized Mdx Player");
    }

    /**
     * Id of the current video playing.  Includes Shorts and YouTube Stories.
     *
     * @return The id of the video. Empty string if not set yet.
     */
    @NonNull
    public static String getVideoId() {
        return videoId;
    }

    /**
     * Injection point.
     *
     * @param newlyLoadedVideoId id of the current video
     */
    public static void setVideoId(@NonNull String newlyLoadedVideoId) {
        if (Objects.equals(newlyLoadedVideoId, videoId)) {
            return;
        }
        Logger.printDebug(() -> "New video id: " + newlyLoadedVideoId);
        videoId = newlyLoadedVideoId;
    }

    /**
     * Differs from {@link #videoId} as this is the video id for the
     * last player response received, which may not be the last video opened.
     * <p>
     * If Shorts are loading the background, this commonly will be
     * different from the Short that is currently on screen.
     * <p>
     * For most use cases, you should instead use {@link #getVideoId()}.
     *
     * @return The id of the last video loaded, or an empty string if no videos have been loaded yet.
     */
    @NonNull
    public static String getPlayerResponseVideoId() {
        return playerResponseVideoId;
    }

    /**
     * @return If the last player response video id was a Sample.
     */
    public static boolean lastPlayerResponseIsSample() {
        return playerResponseVideoIdIsSample;
    }

    /**
     * Injection point.  Called off the main thread.
     *
     * @param videoId The id of the last video loaded.
     */
    public static void setPlayerResponseVideoId(@NonNull String videoId) {
        if (!playerResponseVideoId.equals(videoId)) {
            playerResponseVideoId = videoId;
        }
    }

    /**
     * @return If the player parameter is for a Sample.
     */
    public static boolean parameterIsSample(@Nullable String parameter) {
        return parameter != null && parameter.startsWith(SAMPLES_PLAYER_PARAMETERS);
    }

    /**
     * Injection point.
     */
    @Nullable
    public static String newPlayerResponseParameter(@NonNull String videoId, @Nullable String playerParameter) {
        playerResponseVideoIdIsSample = parameterIsSample(playerParameter);
        Logger.printDebug(() -> "videoId: " + videoId + ", playerParameter: " + playerParameter);

        return playerParameter; // Return the original value since we are observing and not modifying.
    }

    /**
     * Seek on the current video.
     * Does not function for playback of Shorts.
     * <p>
     * Caution: If called from a videoTimeHook() callback,
     * this will cause a recursive call into the same videoTimeHook() callback.
     *
     * @param seekTime The millisecond to seek the video to.
     * @return if the seek was successful
     */
    public static boolean seekTo(final long seekTime) {
        Utils.verifyOnMainThread();
        try {
            final long videoLength = getVideoLength();
            final long videoTime = getVideoTime();
            final long adjustedSeekTime = getAdjustedSeekTime(seekTime, videoLength);

            if (videoTime <= 0 || videoLength <= 0) {
                Logger.printDebug(() -> "Skipping seekTo as the video is not initialized");
                return false;
            }

            Logger.printDebug(() -> "Seeking to: " + getFormattedTimeStamp(adjustedSeekTime));

            // Try regular playback controller first, and it will not succeed if casting.
            if (overrideVideoTime(adjustedSeekTime)) return true;
            Logger.printDebug(() -> "seekTo did not succeeded. Trying MXD.");
            // Else the video is loading or changing videos, or video is casting to a different device.

            // Try calling the seekTo method of the MDX player director (called when casting).
            // The difference has to be a different second mark in order to avoid infinite skip loops
            // as the Lounge API only supports seconds.
            if (adjustedSeekTime / 1000 == videoTime / 1000) {
                Logger.printDebug(() -> "Skipping seekTo for MDX because seek time is too small "
                        + "(" + (adjustedSeekTime - videoTime) + "ms)");
                return false;
            }

            return overrideMDXVideoTime(adjustedSeekTime);
        } catch (Exception ex) {
            Logger.printException(() -> "Failed to seek", ex);
            return false;
        }
    }

    // Prevent issues such as play/pause button or autoplay not working.
    private static long getAdjustedSeekTime(final long seekTime, final long videoLength) {
        // If the user skips to a section that is 500 ms before the video length,
        // it will get stuck in a loop.
        if (videoLength - seekTime > 500) {
            return seekTime;
        } else {
            // Otherwise, just skips to a time longer than the video length.
            // Paradoxically, if user skips to a section much longer than the video length, does not get stuck in a loop.
            return Integer.MAX_VALUE;
        }
    }

    /**
     * @return The current playback speed.
     */
    public static float getPlaybackSpeed() {
        return playbackSpeed;
    }

    /**
     * Injection point.
     *
     * @param newlyLoadedPlaybackSpeed The current playback speed.
     */
    public static void setPlaybackSpeed(float newlyLoadedPlaybackSpeed) {
        playbackSpeed = newlyLoadedPlaybackSpeed;
    }

    /**
     * Length of the current video playing.  Includes Shorts.
     *
     * @return The length of the video in milliseconds.
     * If the video is not yet loaded, or if the video is playing in the background with no video visible,
     * then this returns zero.
     */
    public static long getVideoLength() {
        return videoLength;
    }

    /**
     * Injection point.
     *
     * @param length The length of the video in milliseconds.
     */
    public static void setVideoLength(final long length) {
        if (videoLength != length) {
            videoLength = length;
        }
    }

    /**
     * Playback time of the current video playing.  Includes Shorts.
     * <p>
     * Value will lag behind the actual playback time by a variable amount based on the playback speed.
     * <p>
     * If playback speed is 2.0x, this value may be up to 2000ms behind the actual playback time.
     * If playback speed is 1.0x, this value may be up to 1000ms behind the actual playback time.
     * If playback speed is 0.5x, this value may be up to 500ms behind the actual playback time.
     * Etc.
     *
     * @return The time of the video in milliseconds. -1 if not set yet.
     */
    public static long getVideoTime() {
        return videoTime;
    }

    /**
     * Injection point.
     * Called on the main thread every 1000ms.
     *
     * @param currentPlaybackTime The current playback time of the video in milliseconds.
     */
    public static void setVideoTime(final long currentPlaybackTime) {
        videoTime = currentPlaybackTime;
    }

    /**
     * Overrides the current quality.
     * Rest of the implementation added by patch.
     */
    public static void overrideVideoQuality(int qualityOverride) {
        Logger.printDebug(() -> "Overriding video quality to: " + qualityOverride);
    }

    /**
     * Overrides the current video time by seeking.
     * Rest of the implementation added by patch.
     */
    public static boolean overrideVideoTime(final long seekTime) {
        // These instructions are ignored by patch.
        Logger.printDebug(() -> "Seeking to " + seekTime);
        return false;
    }

    /**
     * Overrides the current video time by seeking. (MDX player)
     * Rest of the implementation added by patch.
     */
    public static boolean overrideMDXVideoTime(final long seekTime) {
        // These instructions are ignored by patch.
        Logger.printDebug(() -> "Seeking to " + seekTime);
        return false;
    }

}
