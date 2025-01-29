package app.revanced.extension.youtube.utils;

import static app.revanced.extension.shared.utils.ResourceUtils.getStringArray;
import static app.revanced.extension.shared.utils.StringRef.str;
import static app.revanced.extension.youtube.patches.video.PlaybackSpeedPatch.userSelectedPlaybackSpeed;

import app.revanced.extension.youtube.shared.ShortsPlayerState;

import android.app.AlertDialog;
import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import app.revanced.extension.shared.settings.EnumSetting;
import app.revanced.extension.shared.utils.IntentUtils;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.youtube.patches.shorts.ShortsRepeatStatePatch.ShortsLoopBehavior;
import app.revanced.extension.youtube.patches.video.CustomPlaybackSpeedPatch;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.settings.preference.ExternalDownloaderPlaylistPreference;
import app.revanced.extension.youtube.settings.preference.ExternalDownloaderVideoLongPressPreference;
import app.revanced.extension.youtube.settings.preference.ExternalDownloaderVideoPreference;
import app.revanced.extension.youtube.shared.PlaylistIdPrefix;
import app.revanced.extension.youtube.shared.VideoInformation;

@SuppressWarnings("unused")
public class VideoUtils extends IntentUtils {
    private static final String CHANNEL_URL = "https://www.youtube.com/channel/";
    private static final String PLAYLIST_URL = "https://www.youtube.com/playlist?list=";
    private static final String VIDEO_URL = "https://youtu.be/";
    private static final String VIDEO_SCHEME_INTENT_FORMAT = "vnd.youtube://%s?start=%d";
    private static final String VIDEO_SCHEME_LINK_FORMAT = "https://youtu.be/%s?t=%d";
    private static final AtomicBoolean isExternalDownloaderLaunched = new AtomicBoolean(false);

    private static String getChannelUrl(String channelId) {
        return CHANNEL_URL + channelId;
    }

    private static String getPlaylistUrl(String playlistId) {
        return PLAYLIST_URL + playlistId;
    }

    private static String getVideoUrl(String videoId) {
        return getVideoUrl(videoId, false);
    }

    private static String getVideoUrl(boolean withTimestamp) {
        return getVideoUrl(VideoInformation.getVideoId(), withTimestamp);
    }

    public static String getVideoUrl(String videoId, boolean withTimestamp) {
        StringBuilder builder = new StringBuilder(VIDEO_URL);
        builder.append(videoId);
        final long currentVideoTimeInSeconds = VideoInformation.getVideoTimeInSeconds();
        if (withTimestamp && currentVideoTimeInSeconds > 0) {
            builder.append("?t=");
            builder.append(currentVideoTimeInSeconds);
        }
        return builder.toString();
    }

    private static String getVideoScheme(String videoId, boolean isShorts) {
        return String.format(
                Locale.ENGLISH,
                isShorts ? VIDEO_SCHEME_INTENT_FORMAT : VIDEO_SCHEME_LINK_FORMAT,
                videoId,
                VideoInformation.getVideoTimeInSeconds()
        );
    }

    public static void copyUrl(boolean withTimestamp) {
        copyUrl(getVideoUrl(withTimestamp), withTimestamp);
    }

    public static void copyUrl(String videoUrl, boolean withTimestamp) {
        setClipboard(videoUrl, withTimestamp
                ? str("revanced_share_copy_url_timestamp_success")
                : str("revanced_share_copy_url_success")
        );
    }

    public static void copyTimeStamp() {
        final String timeStamp = getTimeStamp(VideoInformation.getVideoTime());
        setClipboard(timeStamp, str("revanced_share_copy_timestamp_success", timeStamp));
    }

    public static void launchVideoExternalDownloader() {
        launchVideoExternalDownloader(VideoInformation.getVideoId());
    }

    public static void launchVideoExternalDownloader(@NonNull String videoId) {
        try {
            final String downloaderPackageName = ExternalDownloaderVideoPreference.getExternalDownloaderPackageName();
            if (ExternalDownloaderVideoPreference.checkPackageIsDisabled()) {
                return;
            }

            isExternalDownloaderLaunched.compareAndSet(false, true);
            launchExternalDownloader(getVideoUrl(videoId), downloaderPackageName);
        } catch (Exception ex) {
            Logger.printException(() -> "launchExternalDownloader failure", ex);
        } finally {
            runOnMainThreadDelayed(() -> isExternalDownloaderLaunched.compareAndSet(true, false), 500);
        }
    }

    public static void launchLongPressVideoExternalDownloader() {
        launchLongPressVideoExternalDownloader(VideoInformation.getVideoId());
    }

    public static void launchLongPressVideoExternalDownloader(@NonNull String videoId) {
        try {
            final String downloaderPackageName = ExternalDownloaderVideoLongPressPreference.getExternalDownloaderPackageName();
            if (ExternalDownloaderVideoLongPressPreference.checkPackageIsDisabled()) {
                return;
            }

            isExternalDownloaderLaunched.compareAndSet(false, true);
            launchExternalDownloader(getVideoUrl(videoId), downloaderPackageName);
        } catch (Exception ex) {
            Logger.printException(() -> "launchExternalDownloader failure", ex);
        } finally {
            runOnMainThreadDelayed(() -> isExternalDownloaderLaunched.compareAndSet(true, false), 500);
        }
    }

    public static void launchPlaylistExternalDownloader(@NonNull String playlistId) {
        try {
            final String downloaderPackageName = ExternalDownloaderPlaylistPreference.getExternalDownloaderPackageName();
            if (ExternalDownloaderPlaylistPreference.checkPackageIsDisabled()) {
                return;
            }

            isExternalDownloaderLaunched.compareAndSet(false, true);
            launchExternalDownloader(getPlaylistUrl(playlistId), downloaderPackageName);
        } catch (Exception ex) {
            Logger.printException(() -> "launchPlaylistExternalDownloader failure", ex);
        } finally {
            runOnMainThreadDelayed(() -> isExternalDownloaderLaunched.compareAndSet(true, false), 500);
        }
    }

    public static void openChannel(@NonNull String channelId) {
        launchView(getChannelUrl(channelId), getContext().getPackageName());
    }

    public static void openVideo() {
        openVideo(VideoInformation.getVideoId());
    }

    public static void openVideo(@NonNull String videoId) {
        openVideo(videoId, false, null);
    }

    public static void openVideo(@NonNull String videoId, boolean isShorts) {
        openVideo(videoId, isShorts, null);
    }

    public static void openVideo(@NonNull PlaylistIdPrefix playlistIdPrefix) {
        openVideo(VideoInformation.getVideoId(), false, playlistIdPrefix);
    }

    public static void openVideo(@NonNull String videoId, boolean isShorts, @Nullable PlaylistIdPrefix playlistIdPrefix) {
        final StringBuilder sb = new StringBuilder(getVideoScheme(videoId, isShorts));
        // Create playlist with all channel videos.
        if (playlistIdPrefix != null) {
            sb.append("&list=");
            sb.append(playlistIdPrefix.prefixId);
            if (playlistIdPrefix.useChannelId) {
                final String channelId = VideoInformation.getChannelId();
                // Channel id always starts with `UC` prefix
                if (!channelId.startsWith("UC")) {
                    showToastShort(str("revanced_overlay_button_play_all_not_available_toast"));
                    return;
                }
                sb.append(channelId.substring(2));
            } else {
                sb.append(videoId);
            }
        }

        launchView(sb.toString(), getContext().getPackageName());
    }

    /**
     * Pause the media by changing audio focus.
     */
    @SuppressWarnings("deprecation")
    public static void pauseMedia() {
        if (context != null && context.getApplicationContext().getSystemService(Context.AUDIO_SERVICE) instanceof AudioManager audioManager) {
            audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
    }

    public static void showPlaybackSpeedDialog(@NonNull Context context) {
        final String[] playbackSpeedEntries = CustomPlaybackSpeedPatch.getTrimmedListEntries();
        final String[] playbackSpeedEntryValues = CustomPlaybackSpeedPatch.getTrimmedListEntryValues();

        final float playbackSpeed = VideoInformation.getPlaybackSpeed();
        final int index = Arrays.binarySearch(playbackSpeedEntryValues, String.valueOf(playbackSpeed));

        new AlertDialog.Builder(context)
                .setSingleChoiceItems(playbackSpeedEntries, index, (mDialog, mIndex) -> {
                    final float selectedPlaybackSpeed = Float.parseFloat(playbackSpeedEntryValues[mIndex] + "f");
                    VideoInformation.overridePlaybackSpeed(selectedPlaybackSpeed);
                    userSelectedPlaybackSpeed(selectedPlaybackSpeed);
                    mDialog.dismiss();
                })
                .show();
    }

    public static void showShortsPlaybackSpeedDialog(@NonNull Context context) {
        final String[] playbackSpeedEntries = CustomPlaybackSpeedPatch.getTrimmedListEntries();
        final String[] playbackSpeedEntryValues = CustomPlaybackSpeedPatch.getTrimmedListEntryValues();

        final float playbackSpeed = ShortsPlayerState.Companion.getShortsPlaybackSpeed();
        final int index = Arrays.binarySearch(playbackSpeedEntryValues, String.valueOf(playbackSpeed));

        new AlertDialog.Builder(context)
                .setSingleChoiceItems(playbackSpeedEntries, index, (mDialog, mIndex) -> {
                    final float selectedPlaybackSpeed = Float.parseFloat(playbackSpeedEntryValues[mIndex] + "f");
                    VideoInformation.overridePlaybackSpeed(selectedPlaybackSpeed);
                    userSelectedPlaybackSpeed(selectedPlaybackSpeed);
                    ShortsPlayerState.Companion.setShortsPlaybackSpeed(selectedPlaybackSpeed);
                    mDialog.dismiss();
                })
                .show();
    }

    private static int mClickedDialogEntryIndex;

    public static void showShortsRepeatDialog(@NonNull Context context) {
        final EnumSetting<ShortsLoopBehavior> setting = Settings.CHANGE_SHORTS_REPEAT_STATE;
        final String settingsKey = setting.key;

        final String entryKey = settingsKey + "_entries";
        final String entryValueKey = settingsKey + "_entry_values";
        final String[] mEntries = getStringArray(entryKey);
        final String[] mEntryValues = getStringArray(entryValueKey);

        final int findIndex = Arrays.binarySearch(mEntryValues, String.valueOf(setting.get()));
        mClickedDialogEntryIndex = findIndex >= 0 ? findIndex : setting.defaultValue.ordinal();

        new AlertDialog.Builder(context)
                .setTitle(str(settingsKey + "_title"))
                .setSingleChoiceItems(mEntries, mClickedDialogEntryIndex, (dialog, id) -> {
                    mClickedDialogEntryIndex = id;
                    for (ShortsLoopBehavior behavior : ShortsLoopBehavior.values()) {
                        if (behavior.ordinal() == id) setting.save(behavior);
                    }
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    public static void showFlyoutMenu() {
        if (Settings.APPEND_TIME_STAMP_INFORMATION_TYPE.get()) {
            showVideoQualityFlyoutMenu();
        } else {
            showPlaybackSpeedFlyoutMenu();
        }
    }

    public static String getFormattedQualityString(@Nullable String prefix) {
        final String qualityString = VideoInformation.getVideoQualityString();

        return prefix == null ? qualityString : String.format("%s\u2009•\u2009%s", prefix, qualityString);
    }

    public static String getFormattedSpeedString(@Nullable String prefix) {
        final float playbackSpeed = VideoInformation.getPlaybackSpeed();

        final String playbackSpeedString = isRightToLeftTextLayout()
                ? "\u2066x\u2069" + playbackSpeed
                : playbackSpeed + "x";

        return prefix == null ? playbackSpeedString : String.format("%s\u2009•\u2009%s", prefix, playbackSpeedString);
    }

    /**
     * Injection point.
     * Disable PiP mode when an external downloader Intent is started.
     */
    public static boolean getExternalDownloaderLaunchedState(boolean original) {
        return !isExternalDownloaderLaunched.get() && original;
    }

    /**
     * Rest of the implementation added by patch.
     */
    public static void enterFullscreenMode() {
        Logger.printDebug(() -> "Enter fullscreen mode");
    }

    /**
     * Rest of the implementation added by patch.
     */
    public static void exitFullscreenMode() {
        Logger.printDebug(() -> "Exit fullscreen mode");
    }

    /**
     * Rest of the implementation added by patch.
     */
    public static void showPlaybackSpeedFlyoutMenu() {
        Logger.printDebug(() -> "Playback speed flyout menu opened");
    }

    /**
     * Rest of the implementation added by patch.
     */
    public static void showVideoQualityFlyoutMenu() {
        // These instructions are ignored by patch.
        Log.d("Extended: VideoUtils", "Video quality flyout menu opened");
    }
}
