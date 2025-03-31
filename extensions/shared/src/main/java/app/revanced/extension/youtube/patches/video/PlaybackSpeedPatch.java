package app.revanced.extension.youtube.patches.video;

import static app.revanced.extension.shared.utils.StringRef.str;
import static app.revanced.extension.youtube.shared.RootView.isShortsActive;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.BooleanUtils;

import app.revanced.extension.shared.settings.BooleanSetting;
import app.revanced.extension.shared.settings.FloatSetting;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.Utils;
import app.revanced.extension.youtube.patches.utils.PatchStatus;
import app.revanced.extension.youtube.patches.video.requests.MusicRequest;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.shared.VideoInformation;
import app.revanced.extension.youtube.whitelist.Whitelist;

@SuppressWarnings("unused")
public class PlaybackSpeedPatch {
    private static final FloatSetting DEFAULT_PLAYBACK_SPEED =
            Settings.DEFAULT_PLAYBACK_SPEED;
    private static final FloatSetting DEFAULT_PLAYBACK_SPEED_SHORTS =
            Settings.DEFAULT_PLAYBACK_SPEED_SHORTS;

    private static final boolean DISABLE_DEFAULT_PLAYBACK_SPEED_MUSIC =
            Settings.DISABLE_DEFAULT_PLAYBACK_SPEED_MUSIC.get();
    private static final long TOAST_DELAY_MILLISECONDS = 750;
    private static long lastTimeSpeedChanged;
    private static float lastSelectedPlaybackSpeed = 1.0f;

    private static volatile String channelId = "";
    private static volatile String videoId = "";
    private static boolean isLiveStream;

    private static volatile String channelIdShorts = "";
    private static volatile String videoIdShorts = "";
    private static boolean isLiveStreamShorts;

    /**
     * Injection point.
     */
    public static void newVideoStarted(@NonNull String newlyLoadedChannelId, @NonNull String newlyLoadedChannelName,
                                       @NonNull String newlyLoadedVideoId, @NonNull String newlyLoadedVideoTitle,
                                       final long newlyLoadedVideoLength, boolean newlyLoadedLiveStreamValue) {
        if (isShortsActive()) {
            channelIdShorts = newlyLoadedChannelId;
            videoIdShorts = newlyLoadedVideoId;
            isLiveStreamShorts = newlyLoadedLiveStreamValue;

            Logger.printDebug(() -> "newVideoStarted: " + newlyLoadedVideoId);
        } else {
            channelId = newlyLoadedChannelId;
            videoId = newlyLoadedVideoId;
            isLiveStream = newlyLoadedLiveStreamValue;

            Logger.printDebug(() -> "newShortsVideoStarted: " + newlyLoadedVideoId);
        }
    }

    /**
     * Injection point.
     */
    public static void newShortsVideoStarted(@NonNull String newlyLoadedChannelId, @NonNull String newlyLoadedChannelName,
                                             @NonNull String newlyLoadedVideoId, @NonNull String newlyLoadedVideoTitle,
                                             final long newlyLoadedVideoLength, boolean newlyLoadedLiveStreamValue) {
        channelIdShorts = newlyLoadedChannelId;
        videoIdShorts = newlyLoadedVideoId;
        isLiveStreamShorts = newlyLoadedLiveStreamValue;

        Logger.printDebug(() -> "newShortsVideoStarted: " + newlyLoadedVideoId);
    }

    /**
     * Injection point.
     */
    public static void fetchMusicRequest(@NonNull String videoId, boolean isShortAndOpeningOrPlaying) {
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
                        Settings.DISABLE_DEFAULT_PLAYBACK_SPEED_MUSIC_TYPE.get()
                );
            } catch (Exception ex) {
                Logger.printException(() -> "fetchMusicRequest failure", ex);
            }
        }
    }

    /**
     * Injection point.
     */
    public static float getPlaybackSpeed(float playbackSpeed) {
        boolean isShorts = isShortsActive();
        String currentChannelId = isShorts ? channelIdShorts : channelId;
        String currentVideoId = isShorts ? videoIdShorts : videoId;
        boolean currentVideoIsLiveStream = isShorts ? isLiveStreamShorts : isLiveStream;
        boolean currentVideoIsWhitelisted = Whitelist.isChannelWhitelistedPlaybackSpeed(currentChannelId);
        boolean currentVideoIsMusic = !isShorts && isMusic();

        if (currentVideoIsLiveStream || currentVideoIsWhitelisted || currentVideoIsMusic) {
            Logger.printDebug(() -> "changing playback speed to: 1.0");
            VideoInformation.setPlaybackSpeed(1.0f);
            return 1.0f;
        }

        float defaultPlaybackSpeed = isShorts ? DEFAULT_PLAYBACK_SPEED_SHORTS.get() : DEFAULT_PLAYBACK_SPEED.get();

        if (defaultPlaybackSpeed < 0) {
            float finalPlaybackSpeed = isShorts ? playbackSpeed : lastSelectedPlaybackSpeed;
            VideoInformation.overridePlaybackSpeed(finalPlaybackSpeed);
            Logger.printDebug(() -> "changing playback speed to: " + finalPlaybackSpeed);
            return finalPlaybackSpeed;
        } else {
            if (isShorts) {
                VideoInformation.setPlaybackSpeed(defaultPlaybackSpeed);
            }
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
            if (PatchStatus.RememberPlaybackSpeed()) {
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
                    // With the 0.05x menu, if the speed is set by integrations to higher than 2.0x
                    // then the menu will allow increasing without bounds but the max speed is
                    // still capped to under 8.0x.
                    playbackSpeed = Math.min(playbackSpeed, CustomPlaybackSpeedPatch.PLAYBACK_SPEED_MAXIMUM - 0.05f);

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
            } else if (!isShorts) {
                lastSelectedPlaybackSpeed = playbackSpeed;
            }
        } catch (Exception ex) {
            Logger.printException(() -> "userSelectedPlaybackSpeed failure", ex);
        }
    }

    private static boolean isMusic() {
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
