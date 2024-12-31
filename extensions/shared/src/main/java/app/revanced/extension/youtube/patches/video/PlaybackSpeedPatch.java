package app.revanced.extension.youtube.patches.video;

import static app.revanced.extension.shared.utils.StringRef.str;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.BooleanUtils;

import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.Utils;
import app.revanced.extension.youtube.patches.utils.PatchStatus;
import app.revanced.extension.youtube.patches.video.requests.MusicRequest;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.shared.VideoInformation;
import app.revanced.extension.youtube.whitelist.Whitelist;

@SuppressWarnings("unused")
public class PlaybackSpeedPatch {
    private static final boolean DISABLE_DEFAULT_PLAYBACK_SPEED_MUSIC =
            Settings.DISABLE_DEFAULT_PLAYBACK_SPEED_MUSIC.get();
    private static final long TOAST_DELAY_MILLISECONDS = 750;
    private static long lastTimeSpeedChanged;
    private static boolean isLiveStream;

    /**
     * Injection point.
     */
    public static void newVideoStarted(@NonNull String newlyLoadedChannelId, @NonNull String newlyLoadedChannelName,
                                       @NonNull String newlyLoadedVideoId, @NonNull String newlyLoadedVideoTitle,
                                       final long newlyLoadedVideoLength, boolean newlyLoadedLiveStreamValue) {
        isLiveStream = newlyLoadedLiveStreamValue;
        Logger.printDebug(() -> "newVideoStarted: " + newlyLoadedVideoId);

        final float defaultPlaybackSpeed = getDefaultPlaybackSpeed(newlyLoadedChannelId, newlyLoadedVideoId);
        Logger.printDebug(() -> "overridePlaybackSpeed: " + defaultPlaybackSpeed);

        VideoInformation.overridePlaybackSpeed(defaultPlaybackSpeed);
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
    public static float getPlaybackSpeedInShorts(final float playbackSpeed) {
        if (VideoInformation.lastPlayerResponseIsShort() &&
                Settings.ENABLE_DEFAULT_PLAYBACK_SPEED_SHORTS.get()
        ) {
            float defaultPlaybackSpeed = getDefaultPlaybackSpeed(VideoInformation.getChannelId(), null);
            Logger.printDebug(() -> "overridePlaybackSpeed in Shorts: " + defaultPlaybackSpeed);

            return defaultPlaybackSpeed;
        }

        return playbackSpeed;
    }

    /**
     * Injection point.
     * Called when user selects a playback speed.
     *
     * @param playbackSpeed The playback speed the user selected
     */
    public static void userSelectedPlaybackSpeed(float playbackSpeed) {
        try {
            if (PatchStatus.RememberPlaybackSpeed() &&
                    Settings.REMEMBER_PLAYBACK_SPEED_LAST_SELECTED.get()) {
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

                    if (Settings.DEFAULT_PLAYBACK_SPEED.get() == finalPlaybackSpeed) {
                        // User changed to a different speed and immediately changed back.
                        // Or the user is going past 8.0x in the glitched out 0.05x menu.
                        return;
                    }
                    Settings.DEFAULT_PLAYBACK_SPEED.save(finalPlaybackSpeed);

                    if (!Settings.REMEMBER_PLAYBACK_SPEED_LAST_SELECTED_TOAST.get()) {
                        return;
                    }
                    Utils.showToastShort(str("revanced_remember_playback_speed_toast", (finalPlaybackSpeed + "x")));
                }, TOAST_DELAY_MILLISECONDS);
            }
        } catch (Exception ex) {
            Logger.printException(() -> "userSelectedPlaybackSpeed failure", ex);
        }
    }

    private static float getDefaultPlaybackSpeed(@NonNull String channelId, @Nullable String videoId) {
        return (isLiveStream || Whitelist.isChannelWhitelistedPlaybackSpeed(channelId) || isMusic(videoId))
                ? 1.0f
                : Settings.DEFAULT_PLAYBACK_SPEED.get();
    }

    private static boolean isMusic(@Nullable String videoId) {
        if (DISABLE_DEFAULT_PLAYBACK_SPEED_MUSIC && videoId != null) {
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
