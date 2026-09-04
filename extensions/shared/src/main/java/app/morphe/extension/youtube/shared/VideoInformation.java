/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
 * - inotia00 (https://github.com/inotia00)
 * - Jav1x (https://github.com/Jav1x)
 *
 * Licensed under the GNU General Public License v3.0.
 *
 * ------------------------------------------------------------------------
 * GPLv3 Section 7 – Additional Terms & Attribution Requirements
 * ------------------------------------------------------------------------
 *
 * This file contains substantial original work by the author(s) listed above.
 *
 * In accordance with Section 7 of the GNU General Public License v3.0,
 * the following additional terms apply to this file:
 *
 * 1. Source Credit Preservation (Section 7(b)): This specific copyright notice
 *    and the list of original authors above must be preserved in any copy
 *    or derivative work. You may add your own copyright notice below it,
 *    but you may not remove the original one.
 *
 * 2. Origin & Modification Marking (Section 7(c)): Modified versions must be
 *    clearly marked as such (e.g., by adding a "Modified by" line or a new
 *    copyright notice) and must not be misrepresented as the original work.
 *
 * 3. Version Control Attribution (Section 7(b)): Any ports or substantial
 *    modifications must retain historical authorship credit in version control
 *    systems (e.g., Git), listing original author(s) appropriately and
 *    modifiers as committers or co-authors.
 *
 * 4. User Interface Attribution (Section 7(b)): Any works containing or
 *    derived from this material must maintain a visible credit or
 *    acknowledgment to the original author(s) within the application's
 *    user interface (e.g., in an "About" or "Credits" section).
 */

/*
 * Portions of this file are ported from Morphe:
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.extension.youtube.shared;

import static app.morphe.extension.shared.utils.Utils.getFormattedTimeStamp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.patches.utils.AlwaysRepeatPatch;
import app.morphe.extension.youtube.patches.video.CustomPlaybackSpeedPatch;
import app.morphe.extension.youtube.patches.video.PlaybackSpeedPatch;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.utils.VideoUtils;

/**
 * Hooking class for the current playing video.
 */
@SuppressWarnings("all")
public final class VideoInformation {
    public interface ExoPlayerImpl {
        void patch_setPlaybackParameters(float speed, float pitch);
        void patch_setPlayWhenReady(boolean playing);
    }

    public interface PlaybackSpeedMenuInterface {
        void patch_setSpeed(float speed);
    }

    private static volatile PlaybackSpeedMenuInterface currentPlaybackSpeedMenu;

    public static void setPlaybackSpeedMenu(PlaybackSpeedMenuInterface menu) {
        currentPlaybackSpeedMenu = menu;
    }

    public static PlaybackSpeedMenuInterface getPlaybackSpeedMenu() {
        return currentPlaybackSpeedMenu;
    }

    private static final float DEFAULT_YOUTUBE_PLAYBACK_SPEED = 1.0f;
    /**
     * Prefix present in all Short player parameters signature.
     */
    private static final String SHORTS_PLAYER_PARAMETERS = "8AEB";
    /**
     * Prefix that presents in the player parameter signature when a user manually opens a YouTube Mix and plays a video included in the YouTube Mix.
     */
    private static final String YOUTUBE_MIX_PLAYER_PARAMETERS = "8AUB";
    /**
     * Prefix present in all YouTube Mix (auto-generated playlist) playlist id.
     */
    private static final String YOUTUBE_MIX_PLAYLIST_ID_PREFIX = "RD";

    @NonNull
    private static String channelId = "";
    @NonNull
    private static String channelName = "";
    @NonNull
    private static String videoId = "";
    @NonNull
    private static String videoTitle = "";
    private static long videoLength = 0;
    private static boolean videoIsLiveStream;
    private static long videoTime = -1;

    /**
     * Whether the regular player has ever been opened.
     */
    private static boolean playerInitialized = false;

    @NonNull
    private static volatile String playerResponsePlaylistId = "";
    @NonNull
    private static volatile String playerResponseVideoId = "";
    private static volatile boolean playerResponseVideoIdIsShort;
    private static volatile boolean videoIdIsShort;
    private static volatile boolean playerResponseVideoIdIsAutoGeneratedMixPlaylist;

    private static Long mainVideoLikeCount = null;
    private static boolean isOriginalLikeCountPrecise = false;

    /**
     * Gets the cached original like count of the current video (as the base unliked count).
     *
     * @return The cached like count, or null if not parsed yet.
     */
    @Nullable
    public static Long getOriginalLikeCount() {
        return mainVideoLikeCount;
    }

    /**
     * Sets the original like count of the video, defaulting to imprecise.
     *
     * @param count The like count to set.
     */
    public static void setOriginalLikeCount(@Nullable Long count) {
        setOriginalLikeCount(count, false);
    }

    /**
     * Sets the original like count of the video, with a precision flag.
     * If a precise count has already been set, imprecise counts will be ignored to prevent
     * overwriting exact values with lower-precision rounded values (e.g. 5,497,263 being overwritten by 5,400,000).
     *
     * @param count   The like count to set.
     * @param precise Whether this is a high-precision exact count parsed from content description.
     */
    public static void setOriginalLikeCount(@Nullable Long count, boolean precise) {
        if (count != null) {
            if (isOriginalLikeCountPrecise && !precise) {
                Logger.printDebug(() -> "setOriginalLikeCount: Ignored imprecise count " + count + " because a precise count is already set.");
                return;
            }
            mainVideoLikeCount = count;
            isOriginalLikeCountPrecise = precise;
            Logger.printDebug(() -> "setOriginalLikeCount: " + count + " (precise=" + precise + ") for video: " + getVideoId());
        }
    }

    /**
     * The current playback speed
     */
    private static float playbackSpeed = DEFAULT_YOUTUBE_PLAYBACK_SPEED;

    public static final float DEFAULT_PLAYBACK_AUDIO_PITCH = 1.0f;
    public static final float PLAYBACK_AUDIO_PITCH_MAXIMUM = 8.0f;

    private static float playbackAudioPitch = DEFAULT_PLAYBACK_AUDIO_PITCH;
    private static String playbackAudioPitchFormattedString = "1.0x (0.0st)";

    private static final List<Runnable> playbackSpeedChangeListeners = new CopyOnWriteArrayList<>();
    private static final List<Consumer<Float>> playbackAudioPitchChangeListeners = new CopyOnWriteArrayList<>();
    private static WeakReference<ExoPlayerImpl> exoPlayerImplRef = new WeakReference<>(null);

    private static final double LOG_2 = Math.log(2.0);
    private static final double SEMITONES_PER_OCTAVE = 12.0;

    /**
     * Add a listener that is run when playback speed changes (setPlaybackSpeed or overridePlaybackSpeed).
     * Used by VOT to apply the new speed to the translation player.
     */
    public static void addOnPlaybackSpeedChangeListener(Runnable listener) {
        if (listener != null) playbackSpeedChangeListeners.add(listener);
    }

    public static void removeOnPlaybackSpeedChangeListener(Runnable listener) {
        if (listener != null) playbackSpeedChangeListeners.remove(listener);
    }

    public static void addOnPlaybackAudioPitchChangeListener(Consumer<Float> listener) {
        if (listener != null) playbackAudioPitchChangeListeners.add(listener);
    }

    public static void removeOnPlaybackAudioPitchChangeListener(Consumer<Float> listener) {
        if (listener != null) playbackAudioPitchChangeListeners.remove(listener);
    }

    /**
     * Injection point.
     */
    public static void initializeExoPlayerImpl(ExoPlayerImpl exoPlayerImpl) {
        try {
            Logger.printDebug(() -> "Initializing ExoPlayerImpl: " + exoPlayerImpl);
            exoPlayerImplRef = new WeakReference<>(exoPlayerImpl);
        } catch (Exception ex) {
            Logger.printException(() -> "Failed to initialize ExoPlayer", ex);
        }
    }

    /**
     * Injection point.
     */
    public static void initialize() {
        videoTime = -1;
        videoLength = 0;
        playbackSpeed = DEFAULT_YOUTUBE_PLAYBACK_SPEED;
        playbackAudioPitch = DEFAULT_PLAYBACK_AUDIO_PITCH;
        Logger.printDebug(() -> "Initialized Player");
    }

    /**
     * Injection point.
     */
    public static void initializeMdx() {
        Logger.printDebug(() -> "Initialized Mdx Player");
    }

    public static boolean seekTo(final long seekTime) {
        return seekTo(seekTime, getVideoLength());
    }

    /**
     * Seek on the current video.
     * Does not function for playback of Shorts.
     * <p>
     * Caution: If called from a videoTimeHook() callback,
     * this will cause a recursive call into the same videoTimeHook() callback.
     *
     * @param seekTime The seekTime to seek the video to.
     * @return true if the seek was successful.
     */
    public static boolean seekTo(final long seekTime, final long videoLength) {
        Utils.verifyOnMainThread();
        try {
            final long videoTime = getVideoTime();
            final long adjustedSeekTime = getAdjustedSeekTime(seekTime, videoLength);

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
        }

        // Both the current video time and the seekTo are in the last 500ms of the video.
        if (AlwaysRepeatPatch.alwaysRepeatEnabled()) {
            // If always-repeat is turned on, just skips to time 0.
            return 0;
        } else {
            // Otherwise, just skips to a time longer than the video length.
            // Paradoxically, if user skips to a section much longer than the video length, does not get stuck in a loop.
            return Integer.MAX_VALUE;
        }
    }

    /**
     * Seeks a relative amount.  Should always be used over {@link #seekTo(long)}
     * when the desired seek time is an offset of the current time.
     *
     * @noinspection UnusedReturnValue
     */
    public static boolean seekToRelative(long seekTime) {
        Utils.verifyOnMainThread();
        try {
            Logger.printDebug(() -> "Seeking relative to: " + seekTime);

            // Try regular playback controller first, and it will not succeed if casting.
            if (overrideVideoTimeRelative(seekTime)) return true;
            Logger.printDebug(() -> "seekToRelative did not succeeded. Trying MXD.");

            // Adjust the fine adjustment function so it's at least 1 second before/after.
            // Otherwise the fine adjustment will do nothing when casting.
            final long adjustedSeekTime = seekTime < 0
                    ? Math.min(seekTime, -1000)
                    : Math.max(seekTime, 1000);

            return overrideMDXVideoTimeRelative(adjustedSeekTime);
        } catch (Exception ex) {
            Logger.printException(() -> "Failed to seek relative", ex);
            return false;
        }
    }

    /**
     * Injection point.
     */
    public static void newVideoStarted(@NonNull String newlyLoadedChannelId, @NonNull String newlyLoadedChannelName,
                                       @NonNull String newlyLoadedVideoId, @NonNull String newlyLoadedVideoTitle,
                                       final long newlyLoadedVideoLength, boolean newlyLoadedLiveStreamValue) {
        if (!playerInitialized &&
                PlayerType.getCurrent() != PlayerType.INLINE_MINIMAL) {
            playerInitialized = true;
        }
    }

    /**
     * Injection point used by the YouTube 21.04 player-response path.
     */
    public static void setChannelId(@Nullable String newlyLoadedChannelId) {
        channelId = newlyLoadedChannelId != null ? newlyLoadedChannelId : "";
    }

    /**
     * Injection point used by the YouTube 21.04 player-response path.
     */
    public static void setChannelName(@Nullable String newlyLoadedChannelName) {
        channelName = newlyLoadedChannelName != null ? newlyLoadedChannelName : "";
    }

    public static boolean isPlayerInitialized() {
        return playerInitialized;
    }

    /**
     * Injection point.
     *
     * @param newlyLoadedChannelId       id of the current channel.
     * @param newlyLoadedChannelName     name of the current channel.
     * @param newlyLoadedVideoId         id of the current video.
     * @param newlyLoadedVideoTitle      title of the current video.
     * @param newlyLoadedVideoLength     length of the video in milliseconds.
     * @param newlyLoadedLiveStreamValue whether the current video is a livestream.
     */
    public static void setVideoInformation(@NonNull String newlyLoadedChannelId, @NonNull String newlyLoadedChannelName,
                                           @NonNull String newlyLoadedVideoId, @NonNull String newlyLoadedVideoTitle,
                                           final long newlyLoadedVideoLength, boolean newlyLoadedLiveStreamValue) {
        if (videoId.equals(newlyLoadedVideoId))
            return;

        mainVideoLikeCount = null;
        isOriginalLikeCountPrecise = false;

        channelId = newlyLoadedChannelId;
        channelName = newlyLoadedChannelName;
        videoId = newlyLoadedVideoId;
        videoTitle = newlyLoadedVideoTitle;
        videoLength = newlyLoadedVideoLength;
        videoIsLiveStream = newlyLoadedLiveStreamValue;

        Logger.printDebug(() ->
                "channelId='" +
                        newlyLoadedChannelId +
                        "'\nchannelName='" +
                        newlyLoadedChannelName +
                        "'\nvideoId='" +
                        newlyLoadedVideoId +
                        "'\nvideoTitle='" +
                        newlyLoadedVideoTitle +
                        "'\nvideoLength=" +
                        getFormattedTimeStamp(newlyLoadedVideoLength) +
                        "videoIsLiveStream='" +
                        newlyLoadedLiveStreamValue +
                        "'"
        );
    }

    /**
     * Injection point used by the YouTube 21.04 player seekbar path.
     *
     * @param newlyLoadedVideoLength length of the current video in milliseconds
     */
    public static void setVideoLength(final long newlyLoadedVideoLength) {
        videoLength = newlyLoadedVideoLength;
    }

    /**
     * Injection point.
     *
     * @param newlyLoadedVideoId id of the current video
     */
    public static void setVideoId(@NonNull String newlyLoadedVideoId) {
        if (videoId.equals(newlyLoadedVideoId))
            return;

        mainVideoLikeCount = null;
        isOriginalLikeCountPrecise = false;

        videoId = newlyLoadedVideoId;
    }

    /**
     * Id of the last video opened.  Includes Shorts.
     *
     * @return The id of the video, or an empty string if no videos have been opened yet.
     */
    @NonNull
    public static String getVideoId() {
        return videoId;
    }

    /**
     * Channel Name of the last video opened.  Includes Shorts.
     *
     * @return The channel name of the video.
     */
    @NonNull
    public static String getChannelName() {
        return channelName;
    }

    /**
     * ChannelId of the last video opened.  Includes Shorts.
     *
     * @return The channel id of the video.
     */
    @NonNull
    public static String getChannelId() {
        return channelId;
    }

    public static boolean getLiveStreamState() {
        return videoIsLiveStream;
    }

    /**
     * This is the playlistId of the player response, but since Shorts does not support playlists, it is the same as the current playlistId.
     *
     * @return The playlist id of the video.
     */
    @NonNull
    public static String getPlaylistId() {
        return playerResponsePlaylistId;
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
     * @return If the last player response video id was a Short.
     * Includes Shorts shelf items appearing in the feed that are not opened.
     * @see #lastVideoIdIsShort()
     */
    public static boolean lastPlayerResponseIsShort() {
        return playerResponseVideoIdIsShort;
    }

    /**
     * @return If the last player response video id _that was opened_ was a Short.
     */
    public static boolean lastVideoIdIsShort() {
        return videoIdIsShort;
    }

    /**
     * @return If the last player response video id was an auto-generated YouTube Mix.
     */
    public static boolean lastPlayerResponseIsAutoGeneratedMixPlaylist() {
        return playerResponseVideoIdIsAutoGeneratedMixPlaylist;
    }

    /**
     * @return If the player parameters are for a Short.
     */
    public static boolean playerParametersAreShort(@Nullable String playerParameter) {
        return playerParameter != null && playerParameter.startsWith(SHORTS_PLAYER_PARAMETERS);
    }

    /**
     * @return Whether given id belongs to a YouTube Mix.
     */
    private static boolean isYoutubeMixId(@Nullable final String playlistId) {
        return playlistId != null && playlistId.startsWith(YOUTUBE_MIX_PLAYLIST_ID_PREFIX);
    }

    /**
     * Whether the user manually opened a YouTube Mix.
     */
    public static boolean isMixPlaylistsOpenedByUser(String parameter) {
        return parameter != null && (parameter.isEmpty() || parameter.startsWith(YOUTUBE_MIX_PLAYER_PARAMETERS));
    }

    /**
     * Injection point.
     */
    @Nullable
    public static String newPlayerResponseParameter(@NonNull String videoId, @Nullable String playerParameter,
                                                    @Nullable String playlistId, boolean isShortAndOpeningOrPlaying) {
        final boolean isShort = playerParametersAreShort(playerParameter);
        playerResponseVideoIdIsShort = isShort;
        if (!isShort || isShortAndOpeningOrPlaying) {
            if (videoIdIsShort != isShort) {
                videoIdIsShort = isShort;
            }
        }
        // Typically, Shorts players do not support playlists, so this check may not be necessary.
        if (!isShort) {
            if (playlistId == null || playlistId.isEmpty()) {
                playlistId = "";
            }
            if (!playerResponsePlaylistId.equals(playlistId)) {
                playerResponsePlaylistId = playlistId;
            }
        }
        playerResponseVideoIdIsAutoGeneratedMixPlaylist = isYoutubeMixId(playlistId) && !isMixPlaylistsOpenedByUser(playerParameter);
        return playerParameter; // Return the original value since we are observing and not modifying.
    }

    /**
     * Listener invoked when a new player response is received (video metadata loaded).
     * Called off the main thread. Used by VOT to start translation after video reload.
     */
    @Nullable
    private static volatile OnPlayerResponseReceivedListener onPlayerResponseReceivedListener;

    /**
     * Sets a one-shot listener for the next player response. Cleared after invocation or when set to null.
     */
    public static void setOnPlayerResponseReceivedListener(@Nullable OnPlayerResponseReceivedListener listener) {
        onPlayerResponseReceivedListener = listener;
    }

    /**
     * Listener for when player response is received. Called off the main thread.
     */
    @FunctionalInterface
    public interface OnPlayerResponseReceivedListener {
        void onPlayerResponseReceived(@NonNull String videoId);
    }

    /**
     * Injection point.  Called off the main thread.
     *
     * @param videoId The id of the last video loaded.
     */
    public static void setPlayerResponseVideoId(@NonNull String videoId, boolean isShortAndOpeningOrPlaying) {
        if (!playerResponseVideoId.equals(videoId)) {
            playerResponseVideoId = videoId;
        }
        OnPlayerResponseReceivedListener listener = onPlayerResponseReceivedListener;
        if (listener != null) {
            onPlayerResponseReceivedListener = null;
            try {
                listener.onPlayerResponseReceived(videoId);
            } catch (Exception e) {
                Logger.printException(() -> "onPlayerResponseReceived failed", e);
            }
        }
    }

    /**
     * @return The current playback speed.
     */
    public static float getPlaybackSpeed() {
        return playbackSpeed;
    }

    /**
     * Injection point & getter.
     * @return The current playback audio pitch.
     */
    public static float getPlaybackAudioPitch() {
        if (!Settings.ENABLE_PLAYBACK_AUDIO_PITCH.get()) {
            return 1.0f;
        }
        float overridePitch = PlaybackSpeedPatch.getPlaybackAudioPitchOverride();
        if (overridePitch > 0) {
            playbackAudioPitch = overridePitch;
            playbackAudioPitchFormattedString = formatAudioPitchStringX(playbackAudioPitch);
        }
        return playbackAudioPitch;
    }

    /**
     * @param pitch The playback audio pitch value to format.
     * @return pitch formatted as "X.XXx (Nst)" with signed one-decimal semitone offset.
     */
    public static String formatAudioPitchStringX(float pitch) {
        if (!(pitch > 0f)) {
            throw new IllegalArgumentException("pitch must be a positive non infinite value: " + pitch);
        }

        final double semitones = SEMITONES_PER_OCTAVE * (Math.log(pitch) / LOG_2);
        String formattedSemitones = String.format(Locale.US, "%.1f", semitones);
        if (formattedSemitones.equals("-0.0")) {
            formattedSemitones = "0.0";
        }
        String signedSemitones = formattedSemitones.startsWith("-") || formattedSemitones.equals("0.0")
                ? formattedSemitones
                : "+" + formattedSemitones;

        String speed = VideoUtils.formatSpeedStringX(pitch, 2);
        return Utils.isRightToLeftLocale()
                ? String.format(Locale.US, "(%sst) %s", signedSemitones, speed)
                : String.format(Locale.US, "%s (%sst)", speed, signedSemitones);
    }

    /**
     * Records a new playback audio pitch, updates the formatted string, and fires listeners.
     *
     * @return true if the pitch actually changed.
     */
    public static boolean updatePlaybackAudioPitchValue(float pitch) {
        if (!Settings.ENABLE_PLAYBACK_AUDIO_PITCH.get()) {
            pitch = 1.0f;
        }
        if (playbackAudioPitch == pitch) {
            return false;
        }

        playbackAudioPitch = pitch;
        Logger.printDebug(() -> "Audio pitch updated: " + playbackAudioPitch);
        playbackAudioPitchFormattedString = formatAudioPitchStringX(pitch);
        for (Consumer<Float> listener : playbackAudioPitchChangeListeners) {
            try { listener.accept(pitch); } catch (Exception e) { Logger.printException(() -> "Playback audio pitch listener", e); }
        }
        PlaybackSpeedPatch.userSelectedPlaybackAudioPitch(pitch);
        if (!Settings.PLAYBACK_AUDIO_TIME_STRETCHING.get()) {
            if (playbackSpeed != pitch) {
                setPlaybackSpeed(pitch);
            }
        }
        setPlaybackParameters(playbackSpeed, playbackAudioPitch);
        return true;
    }

    public static void setAudioPitch(float currentAudioPitch) {
        Logger.printDebug(() -> "Audio pitch set to: " + currentAudioPitch);
        updatePlaybackAudioPitchValue(currentAudioPitch);
    }

    /**
     * Forcefully changes the playback parameters (speed and pitch) of the current ExoPlayerImpl instance.
     * Avoid using this for just video speed changes, YT won't update in other places.
     */
    public static void setPlaybackParameters(float speed, float pitch) {
        Utils.verifyOnMainThread();

        if (speed <= 0 || speed > CustomPlaybackSpeedPatch.PLAYBACK_SPEED_MAXIMUM) {
            Logger.printException(() -> "Invalid playback speed: " + speed);
            return;
        }
        if (pitch <= 0 || pitch > PLAYBACK_AUDIO_PITCH_MAXIMUM) {
            Logger.printException(() -> "Invalid playback pitch: " + pitch);
            return;
        }

        ExoPlayerImpl exoPlayerImpl = exoPlayerImplRef.get();
        if (exoPlayerImpl != null) {
            exoPlayerImpl.patch_setPlaybackParameters(speed, pitch);
            Logger.printDebug(() -> "Video playbackParameters changed, speed: " + speed + " pitch: " + pitch);
        } else {
            Logger.printDebug(() -> "Cannot change playback parameters, exoPlayerImpl is null");
        }
    }

    /**
     * Changes whether the current ExoPlayer is ready to play.
     *
     * @param playing whether the player should be ready to play
     * @return true if a playback-control method was invoked
     */
    public static boolean setPlayerPlaying(boolean playing) {
        Utils.verifyOnMainThread();

        ExoPlayerImpl exoPlayerImpl = exoPlayerImplRef.get();
        if (exoPlayerImpl == null) {
            Logger.printDebug(() -> "Cannot change playback state, exoPlayerImpl is null");
            return false;
        }

        try {
            exoPlayerImpl.patch_setPlayWhenReady(playing);
            return true;
        } catch (Exception e) {
            Logger.printDebug(() -> "Failed to change playback state: " + e.getMessage());
        }
        return false;
    }

    /**
     * Tries to read the current playback speed from the app's player (playbackSpeedClass / timeUpdateReceiver).
     * Used by VOT to sync translation speed when the user changes speed via any UI (not only the menu we hook).
     *
     * @return Speed &gt; 0 if found, otherwise -1f (use {@link #getPlaybackSpeed()} as fallback).
     */
    public static float getPlaybackSpeedFromPlayer() {
        float v = tryGetSpeedFromObject(getPlaybackSpeedClassRef());
        if (v > 0f) return v;
        Object receiver = getTimeUpdateReceiverRef();
        v = tryGetSpeedFromObject(receiver);
        if (v > 0f) return v;
        if (receiver != null) {
            for (String getterName : new String[]{"getPlayer", "getExoPlayer", "getPlayback", "getWrappedPlayer", "getInnerPlayer", "getAudioComponent", "getController", "getPlaybackController"}) {
                try {
                    java.lang.reflect.Method m = receiver.getClass().getMethod(getterName);
                    if (m.getParameterCount() == 0 && !m.getReturnType().isPrimitive()) {
                        Object child = m.invoke(receiver);
                        v = tryGetSpeedFromObject(child);
                        if (v > 0f) return v;
                    }
                } catch (Exception ignored) { }
            }
        }
        return -1f;
    }

    private static float tryGetSpeedFromObject(Object obj) {
        if (obj == null) return -1f;
        try {
            for (String methodName : new String[]{"getPlaybackSpeed", "getSpeed", "getPlaybackRate", "getCurrentSpeed"}) {
                java.lang.reflect.Method m = findMethod(obj.getClass(), methodName);
                if (m != null && m.getParameterCount() == 0) {
                    Class<?> ret = m.getReturnType();
                    if (ret == float.class || ret == double.class || ret == Float.class || ret == Double.class) {
                        Object result = m.invoke(obj);
                        if (result != null) {
                            float f = ((Number) result).floatValue();
                            if (f > 0f && f <= 10f) return f;
                        }
                    }
                }
            }
            // ExoPlayer/Media3: getPlaybackParameters().getSpeed()
            java.lang.reflect.Method getParams = findMethod(obj.getClass(), "getPlaybackParameters");
            if (getParams != null && getParams.getParameterCount() == 0) {
                Object params = getParams.invoke(obj);
                if (params != null) {
                    float fromParams = tryGetSpeedFromObject(params);
                    if (fromParams > 0f) return fromParams;
                }
            }
            for (String fieldName : new String[]{"playbackSpeed", "speed", "playbackRate", "mSpeed"}) {
                try {
                    java.lang.reflect.Field f = obj.getClass().getDeclaredField(fieldName);
                    f.setAccessible(true);
                    Object val = f.get(obj);
                    if (val instanceof Number) {
                        float speedVal = ((Number) val).floatValue();
                        if (speedVal > 0f && speedVal <= 10f) return speedVal;
                    }
                } catch (NoSuchFieldException ignored) { }
            }
        } catch (Exception ignored) { }
        return -1f;
    }

    private static Object getTimeUpdateReceiverRef() {
        try {
            java.lang.reflect.Field f = VideoInformation.class.getDeclaredField("timeUpdateReceiver");
            f.setAccessible(true);
            return f.get(null);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Injection point.
     *
     * @param newlyLoadedPlaybackSpeed The current playback speed.
     */
    public static void setPlaybackSpeed(float newlyLoadedPlaybackSpeed) {
        if (playbackSpeed != newlyLoadedPlaybackSpeed) {
            Logger.printDebug(() -> "Video speed changed: " + newlyLoadedPlaybackSpeed);
            playbackSpeed = newlyLoadedPlaybackSpeed;
            if (!Settings.PLAYBACK_AUDIO_TIME_STRETCHING.get()) {
                if (playbackAudioPitch != newlyLoadedPlaybackSpeed) {
                    updatePlaybackAudioPitchValue(newlyLoadedPlaybackSpeed);
                }
            }
            for (Runnable r : playbackSpeedChangeListeners) {
                try { r.run(); } catch (Exception e) { Logger.printException(() -> "Playback speed listener", e); }
            }
        }
    }

    /**
     * Title of the current video playing.  Includes Shorts.
     *
     * @return The title of the video.
     */
    public static String getVideoTitle() {
        return videoTitle;
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

    public static long getVideoTimeInSeconds() {
        return videoTime / 1000;
    }

    /**
     * Injection point.
     * Called on the main thread every 100ms.
     *
     * @param time The current playback time of the video in milliseconds.
     */
    public static void setVideoTime(final long time) {
        videoTime = time;
        Logger.printDebug(() -> "setVideoTime: " + getFormattedTimeStamp(time));
    }

    /**
     * @return If the playback is at the end of the video.
     * <p>
     * If video is playing in the background with no video visible,
     * this always returns false (even if the video is actually at the end).
     * <p>
     * This is equivalent to checking for {@link VideoState#ENDED},
     * but can give a more up-to-date result for code calling from some hooks.
     * @see VideoState
     */
    public static boolean isAtEndOfVideo() {
        return videoTime >= videoLength && videoLength > 0;
    }

    /**
     * Overrides the current playback speed.
     * Rest of the implementation added by patch.
     */
    public static void overridePlaybackSpeed(float speedOverride) {
        Logger.printDebug(() -> "Overriding playback speed to: " + speedOverride);
        if (currentPlaybackSpeedMenu != null) {
            try {
                currentPlaybackSpeedMenu.patch_setSpeed(speedOverride);
            } catch (Throwable t) {
                Logger.printException(() -> "Failed to set playback speed on menu", t);
            }
        }
        if (playbackSpeed != speedOverride) {
            playbackSpeed = speedOverride;
            if (!Settings.PLAYBACK_AUDIO_TIME_STRETCHING.get()) {
                if (playbackAudioPitch != speedOverride) {
                    updatePlaybackAudioPitchValue(speedOverride);
                }
            }
            for (Runnable r : playbackSpeedChangeListeners) {
                try { r.run(); } catch (Exception e) { Logger.printException(() -> "Playback speed listener", e); }
            }
        }
    }

    /**
     * Gets the current ExoPlayer volume (0..1).
     * Uses reflection on playbackSpeedClass (set by patch). Used by VOT to restore volume when unmuting.
     *
     * @return current volume or 1.0f if player not available
     */
    public static float getPlayerVolume() {
        Object target = getVolumeTarget();
        if (target == null) return 1.0f;
        try {
            java.lang.reflect.Method getVol = findMethod(target.getClass(), "getVolume");
            if (getVol != null) {
                Object result = getVol.invoke(target);
                if (result instanceof Number) {
                    float v = ((Number) result).floatValue();
                    return (v > 1.0f) ? (v / 100.0f) : v; // normalize 0-100 to 0-1
                }
            }
        } catch (Exception e) {
            Logger.printDebug(() -> "getPlayerVolume: " + e.getMessage());
        }
        return 1.0f;
    }

    /**
     * Sets the ExoPlayer volume (0 = mute, 1 = full). Used by VOT to mute original when translation is playing.
     *
     * @param volume volume in 0..1
     */
    public static void setPlayerVolume(float volume) {
        Object target = getVolumeTarget();
        if (target == null) {
            Logger.printInfo(() -> "setPlayerVolume: no target (playbackSpeedClass/timeUpdateReceiver or resolveVolumeTarget returned null). Mute may not work.");
            return;
        }
        try {
            Class<?> c = target.getClass();
            java.lang.reflect.Method setVol = findMethod(c, "setVolume", float.class);
            if (setVol != null) {
                setVol.invoke(target, volume);
                Logger.printDebug(() -> "setPlayerVolume: set " + volume + " on " + c.getSimpleName());
                return;
            }
            setVol = findMethod(c, "setVolume", int.class);
            if (setVol != null) {
                setVol.invoke(target, Math.round(volume * 100));
                Logger.printDebug(() -> "setPlayerVolume: set (int) " + Math.round(volume * 100) + " on " + c.getSimpleName());
                return;
            }
            setVol = findMethod(c, "setVolume", double.class);
            if (setVol != null) {
                setVol.invoke(target, (double) volume);
                Logger.printDebug(() -> "setPlayerVolume: set (double) on " + c.getSimpleName());
            }
        } catch (Exception e) {
            Logger.printInfo(() -> "setPlayerVolume failed: " + e.getMessage());
        }
    }

    /** Set by patch from the method that reports video time (p0). Used as fallback when playbackSpeedClass is null. */
    private static volatile Object timeUpdateReceiver;

    public static void setTimeUpdateReceiver(Object receiver) {
        timeUpdateReceiver = receiver;
    }

    private static Object getVolumeTarget() {
        // 1) Try playbackSpeedClass (set when speed menu / player is used)
        Object obj = getPlaybackSpeedClassRef();
        if (obj != null) {
            Object target = resolveVolumeTarget(obj);
            if (target != null) return target;
        }
        // 2) Fallback: object from the method that reports video time (set every ~100ms while playing)
        obj = timeUpdateReceiver;
        if (obj != null) {
            Object target = resolveVolumeTarget(obj);
            if (target != null) return target;
        }
        return null;
    }

    private static Object getPlaybackSpeedClassRef() {
        try {
            java.lang.reflect.Field f = VideoInformation.class.getDeclaredField("playbackSpeedClass");
            f.setAccessible(true);
            return f.get(null);
        } catch (Exception e) {
            return null;
        }
    }

    private static Object resolveVolumeTarget(Object obj) {
        if (obj == null) return null;
        Class<?> clazz = obj.getClass();
        if (hasVolumeMethods(clazz)) return obj;
        if (hasSetVolumeOnly(clazz)) return obj;
        String[] getterNames = {
            "getAudioComponent", "getPlayer", "getExoPlayer",
            "getWrappedPlayer", "getInnerPlayer", "getPlayback",
            "getImpl", "getDelegate", "getExoPlayerImpl", "getPlaybackImpl"
        };
        for (String name : getterNames) {
            try {
                for (java.lang.reflect.Method m : clazz.getMethods()) {
                    if (!m.getName().equals(name) || m.getParameterCount() != 0) continue;
                    Class<?> ret = m.getReturnType();
                    if (ret.isPrimitive() || ret == String.class) continue;
                    Object child = m.invoke(obj);
                    if (child != null && (hasVolumeMethods(child.getClass()) || hasSetVolumeOnly(child.getClass())))
                        return child;
                }
            } catch (Exception ignored) { }
        }
        for (java.lang.reflect.Method m : clazz.getMethods()) {
            if (m.getParameterCount() != 0 || m.getReturnType().isPrimitive()) continue;
            String name = m.getName();
            if (!name.startsWith("get") || name.length() < 4) continue;
            try {
                Object child = m.invoke(obj);
                if (child != null && child != obj && (hasVolumeMethods(child.getClass()) || hasSetVolumeOnly(child.getClass())))
                    return child;
            } catch (Exception ignored) { }
        }
        return null;
    }

    private static boolean hasSetVolumeOnly(Class<?> c) {
        return findMethod(c, "setVolume", float.class) != null || findMethod(c, "setVolume", int.class) != null || findMethod(c, "setVolume", double.class) != null;
    }

    private static boolean hasVolumeMethods(Class<?> c) {
        if (findMethod(c, "getVolume") == null) return false;
        return findMethod(c, "setVolume", float.class) != null
                || findMethod(c, "setVolume", int.class) != null
                || findMethod(c, "setVolume", double.class) != null;
    }

    private static java.lang.reflect.Method findMethod(Class<?> c, String name, Class<?>... paramTypes) {
        for (; c != null; c = c.getSuperclass()) {
            try {
                java.lang.reflect.Method m = c.getDeclaredMethod(name, paramTypes);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ignored) { }
        }
        return null;
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

    /**
     * Overrides the current video time by seeking relative.
     * Rest of the implementation added by patch.
     */
    public static boolean overrideVideoTimeRelative(final long seekTime) {
        // These instructions are ignored by patch.
        Logger.printDebug(() -> "Seeking to " + seekTime);
        return false;
    }

    /**
     * Overrides the current video time by seeking relative. (MDX player)
     * Rest of the implementation added by patch.
     */
    public static boolean overrideMDXVideoTimeRelative(final long seekTime) {
        // These instructions are ignored by patch.
        Logger.printDebug(() -> "Seeking to " + seekTime);
        return false;
    }
}
