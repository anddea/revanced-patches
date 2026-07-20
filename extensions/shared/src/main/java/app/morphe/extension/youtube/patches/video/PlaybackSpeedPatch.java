/*
 * Copyright (C) 2024-2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
 * - inotia00 (https://github.com/inotia00)
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

    /**
     * Prevents a delayed music result from overriding a speed selected by the user.
     */
    private static boolean userChangedSpeedForCurrentVideo = false;

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
        userChangedSpeedForCurrentVideo = false;

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
                userChangedSpeedForCurrentVideo = true;
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

    /**
     * Applies a completed music request on the main thread.
     * Results for stale videos and videos whose speed was changed by the user are ignored.
     */
    public static void musicRequestCompleted(String videoId) {
        Utils.runOnMainThread(() -> {
            if (!videoId.equals(PlaybackSpeedPatch.videoId) || userChangedSpeedForCurrentVideo) {
                return;
            }

            synchronized (ignoredPlaybackSpeedVideoIds) {
                if (!ignoredPlaybackSpeedVideoIds.containsKey(videoId)) {
                    lastSelectedPlaybackSpeed = 1.0f;
                    ignoredPlaybackSpeedVideoIds.put(videoId, lastSelectedPlaybackSpeed);

                    VideoInformation.setPlaybackSpeed(lastSelectedPlaybackSpeed);
                    VideoInformation.overridePlaybackSpeed(lastSelectedPlaybackSpeed);

                    Logger.printDebug(() -> "Asynchronously changed playback speed to: 1.0, isMusic: true");
                }
            }
        });
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
