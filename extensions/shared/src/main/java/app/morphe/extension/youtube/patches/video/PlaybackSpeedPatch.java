package app.morphe.extension.youtube.patches.video;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;

import org.apache.commons.lang3.BooleanUtils;

import java.util.LinkedHashMap;
import java.util.Map;

import app.morphe.extension.shared.innertube.utils.AuthUtils;
import app.morphe.extension.shared.settings.BooleanSetting;
import app.morphe.extension.shared.settings.FloatSetting;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.patches.utils.PatchStatus;
import app.morphe.extension.youtube.patches.video.requests.MusicRequest;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.shared.VideoInformation;
import app.morphe.extension.youtube.whitelist.Whitelist;

import static app.morphe.extension.shared.utils.StringRef.str;
import static app.morphe.extension.youtube.shared.RootView.isShortsActive;

@SuppressWarnings("unused")
public class PlaybackSpeedPatch {
    private static final FloatSetting DEFAULT_PLAYBACK_SPEED =
            Settings.DEFAULT_PLAYBACK_SPEED;
    private static final FloatSetting DEFAULT_PLAYBACK_SPEED_SHORTS =
            Settings.DEFAULT_PLAYBACK_SPEED_SHORTS;

    private static final boolean DISABLE_DEFAULT_PLAYBACK_SPEED_MUSIC =
            Settings.DISABLE_DEFAULT_PLAYBACK_SPEED_MUSIC.get();
    private static final boolean DISABLE_DEFAULT_PLAYBACK_SPEED_MUSIC_TYPE =
            DISABLE_DEFAULT_PLAYBACK_SPEED_MUSIC && Settings.DISABLE_DEFAULT_PLAYBACK_SPEED_MUSIC_TYPE.get();
    private static final long TOAST_DELAY_MILLISECONDS = 750;
    private static long lastTimeSpeedChanged;

    /**
     * The last used playback speed.
     * This value is used when the default playback speed is 'Auto'.
     */
    private static float lastSelectedPlaybackSpeed = 1.0f;
    private static float lastSelectedShortsPlaybackSpeed = 1.0f;

    /**
     * The last regular video id.
     */
    private static String videoId = "";

    @GuardedBy("itself")
    private static final Map<String, Float> ignoredPlaybackSpeedVideoIds = new LinkedHashMap<>() {
        private static final int NUMBER_OF_LAST_VIDEO_IDS_TO_TRACK = 3;

        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > NUMBER_OF_LAST_VIDEO_IDS_TO_TRACK;
        }
    };

    /**
     * Injection point.
     * This method is used to reset the playback speed to 1.0 when a general video is started, whether it is a live stream, music, or whitelist.
     */
    public static void newVideoStarted(@NonNull String newlyLoadedChannelId, @NonNull String newlyLoadedChannelName,
                                       @NonNull String newlyLoadedVideoId, @NonNull String newlyLoadedVideoTitle,
                                       final long newlyLoadedVideoLength, boolean newlyLoadedLiveStreamValue) {
        if (isShortsActive()) {
            return;
        }
        if (videoId.equals(newlyLoadedVideoId)) {
            return;
        }
        videoId = newlyLoadedVideoId;

        boolean isMusic = isMusic(newlyLoadedVideoId);
        boolean isWhitelisted = Whitelist.isChannelWhitelistedPlaybackSpeed(newlyLoadedChannelId);

        if (newlyLoadedLiveStreamValue || isMusic || isWhitelisted) {
            synchronized (ignoredPlaybackSpeedVideoIds) {
                if (!ignoredPlaybackSpeedVideoIds.containsKey(newlyLoadedVideoId)) {
                    lastSelectedPlaybackSpeed = 1.0f;
                    ignoredPlaybackSpeedVideoIds.put(newlyLoadedVideoId, lastSelectedPlaybackSpeed);

                    VideoInformation.setPlaybackSpeed(lastSelectedPlaybackSpeed);
                    VideoInformation.overridePlaybackSpeed(lastSelectedPlaybackSpeed);

                    Logger.printDebug(() -> "changing playback speed to: 1.0, isLiveStream: " + newlyLoadedLiveStreamValue +
                            ", isMusic: " + isMusic + ", isWhitelisted: " + isWhitelisted);
                }
            }
        }
    }

    /**
     * Injection point.
     */
    public static void fetchRequest(@NonNull String videoId, boolean isShortAndOpeningOrPlaying) {
        if (DISABLE_DEFAULT_PLAYBACK_SPEED_MUSIC) {
            try {
                final boolean videoIdIsShort = VideoInformation.lastPlayerResponseIsShort();
                // Shorts shelf in home and subscription feed causes player response hook to be called,
                // and the 'is opening/playing' parameter will be false.
                // This hook will be called again when the Short is actually opened.
                if (videoIdIsShort && !isShortAndOpeningOrPlaying) {
                    return;
                }

                MusicRequest.fetchRequestIfNeeded(
                        videoId,
                        DISABLE_DEFAULT_PLAYBACK_SPEED_MUSIC_TYPE,
                        AuthUtils.getRequestHeader()
                );
            } catch (Exception ex) {
                Logger.printException(() -> "fetchRequest failure", ex);
            }
        }
    }

    /**
     * Injection point.
     * This method is called every second for regular videos and Shorts.
     */
    public static float getPlaybackSpeed(float playbackSpeed) {
        boolean isShorts = isShortsActive();
        float defaultPlaybackSpeed = isShorts ? DEFAULT_PLAYBACK_SPEED_SHORTS.get() : DEFAULT_PLAYBACK_SPEED.get();

        if (defaultPlaybackSpeed < 0) { // If the default playback speed is 'Auto', it will be overridden to the last used playback speed.
            float finalPlaybackSpeed = isShorts ? lastSelectedShortsPlaybackSpeed : lastSelectedPlaybackSpeed;
            if (isShorts) {
                VideoInformation.setPlaybackSpeed(lastSelectedShortsPlaybackSpeed);
            } else {
                VideoInformation.overridePlaybackSpeed(lastSelectedPlaybackSpeed);
            }
            Logger.printDebug(() -> "changing playback speed to: " + finalPlaybackSpeed);
            return finalPlaybackSpeed;
        } else { // Otherwise the default playback speed is used.
            synchronized (ignoredPlaybackSpeedVideoIds) {
                if (!isShorts && ignoredPlaybackSpeedVideoIds.containsKey(videoId)) {
                    // For general videos, check whether the default video playback speed should not be applied.
                    Logger.printDebug(() -> "changing playback speed to: 1.0");
                    return 1.0f;
                }
            }

            // Sometimes VideoInformation.overridePlaybackSpeed() method is not used, so manually save the playback speed in VideoInformation.
            VideoInformation.setPlaybackSpeed(defaultPlaybackSpeed);
            Logger.printDebug(() -> "changing playback speed to: " + defaultPlaybackSpeed);
            return defaultPlaybackSpeed;
        }
    }

    /**
     * Injection point.
     * Called when user selects a playback speed.
     *
     * @param playbackSpeed The playback speed the user selected
     */
    public static void userSelectedPlaybackSpeed(float playbackSpeed) {
        try {
            boolean isShorts = isShortsActive();

            // Saves the user-selected playback speed in the method.
            if (isShorts) {
                lastSelectedShortsPlaybackSpeed = playbackSpeed;
            } else {
                lastSelectedPlaybackSpeed = playbackSpeed;
                // If the user has manually changed the playback speed, the whitelist has already been applied.
                // If there is a videoId on the map, it will be removed.
                synchronized (ignoredPlaybackSpeedVideoIds) {
                    ignoredPlaybackSpeedVideoIds.remove(videoId);
                }
            }

            if (PatchStatus.VideoPlayback()) {
                BooleanSetting rememberPlaybackSpeedLastSelectedSetting = isShorts
                        ? Settings.REMEMBER_PLAYBACK_SPEED_SHORTS_LAST_SELECTED
                        : Settings.REMEMBER_PLAYBACK_SPEED_LAST_SELECTED;
                FloatSetting playbackSpeedSetting = isShorts
                        ? DEFAULT_PLAYBACK_SPEED_SHORTS
                        : DEFAULT_PLAYBACK_SPEED;
                BooleanSetting showToastSetting = isShorts
                        ? Settings.REMEMBER_PLAYBACK_SPEED_SHORTS_LAST_SELECTED_TOAST
                        : Settings.REMEMBER_PLAYBACK_SPEED_LAST_SELECTED_TOAST;

                if (rememberPlaybackSpeedLastSelectedSetting.get()) {
                    // With the 0.05x menu, if the speed is set by a patch to higher than 2.0x
                    // then the menu will allow increasing without bounds but the max speed is
                    // still capped to 8.0x.
                    playbackSpeed = Math.min(playbackSpeed, CustomPlaybackSpeedPatch.PLAYBACK_SPEED_MAXIMUM);

                    // Prevent toast spamming if using the 0.05x adjustments.
                    // Show exactly one toast after the user stops interacting with the speed menu.
                    final long now = System.currentTimeMillis();
                    lastTimeSpeedChanged = now;

                    final float finalPlaybackSpeed = playbackSpeed;
                    Utils.runOnMainThreadDelayed(() -> {
                        if (lastTimeSpeedChanged != now) {
                            // The user made additional speed adjustments and this call is outdated.
                            return;
                        }
                        if (playbackSpeedSetting.get() == finalPlaybackSpeed) {
                            // User changed to a different speed and immediately changed back.
                            // Or the user is going past 8.0x in the glitched out 0.05x menu.
                            return;
                        }
                        playbackSpeedSetting.save(finalPlaybackSpeed);

                        if (showToastSetting.get()) {
                            Utils.showToastShort(str(isShorts ? "revanced_remember_playback_speed_toast_shorts" : "revanced_remember_playback_speed_toast", (finalPlaybackSpeed + "x")));
                        }
                    }, TOAST_DELAY_MILLISECONDS);
                }
            }
        } catch (Exception ex) {
            Logger.printException(() -> "userSelectedPlaybackSpeed failure", ex);
        }
    }

    private static boolean isMusic(String videoId) {
        if (DISABLE_DEFAULT_PLAYBACK_SPEED_MUSIC && !videoId.isEmpty()) {
            try {
                MusicRequest request = MusicRequest.getRequestForVideoId(videoId);
                final boolean isMusic = request != null && BooleanUtils.toBoolean(request.getStream());
                Logger.printDebug(() -> "videoId: " + videoId + ", isMusic: " + isMusic);

                return isMusic;
            } catch (Exception ex) {
                Logger.printException(() -> "getMusicRequest failure", ex);
            }
        }

        return false;
    }
}
