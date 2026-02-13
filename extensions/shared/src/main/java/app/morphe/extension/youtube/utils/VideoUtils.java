package app.morphe.extension.youtube.utils;

import static app.morphe.extension.shared.utils.BaseThemeUtils.getDialogBackgroundColor;
import static app.morphe.extension.shared.utils.ResourceUtils.getString;
import static app.morphe.extension.shared.utils.ResourceUtils.getStringArray;
import static app.morphe.extension.shared.utils.StringRef.str;
import static app.morphe.extension.shared.utils.Utils.dipToPixels;
import static app.morphe.extension.youtube.patches.video.CustomPlaybackSpeedPatch.PLAYBACK_SPEED_MAXIMUM;
import static app.morphe.extension.youtube.patches.video.CustomPlaybackSpeedPatch.SPEED_ADJUSTMENT_CHANGE;
import static app.morphe.extension.youtube.patches.video.PlaybackSpeedPatch.userSelectedPlaybackSpeed;
import static app.morphe.extension.youtube.patches.video.VideoQualityPatch.AUTOMATIC_VIDEO_QUALITY_VALUE;
import static app.morphe.extension.youtube.patches.video.VideoQualityPatch.setCurrentQuality;
import static app.morphe.extension.youtube.utils.ThemeUtils.getAdjustedBackgroundColor;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.media.AudioManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.libraries.youtube.innertube.model.media.VideoQuality;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import app.morphe.extension.shared.settings.EnumSetting;
import app.morphe.extension.shared.ui.SheetBottomDialog;
import app.morphe.extension.shared.utils.BaseThemeUtils;
import app.morphe.extension.shared.utils.IntentUtils;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.ResourceUtils;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.patches.shorts.ShortsRepeatStatePatch.ShortsLoopBehavior;
import app.morphe.extension.youtube.patches.video.CustomPlaybackSpeedPatch;
import app.morphe.extension.youtube.patches.video.CustomPlaybackSpeedPatch.PlaybackSpeedMenuType;
import app.morphe.extension.youtube.patches.video.PlaybackSpeedPatch;
import app.morphe.extension.youtube.patches.video.VideoQualityPatch;
import app.morphe.extension.youtube.patches.video.VideoQualityPatch.VideoQualityMenuInterface;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.settings.preference.ExternalDownloaderPlaylistPreference;
import app.morphe.extension.youtube.settings.preference.ExternalDownloaderVideoLongPressPreference;
import app.morphe.extension.youtube.settings.preference.ExternalDownloaderVideoPreference;
import app.morphe.extension.youtube.shared.PlaylistIdPrefix;
import app.morphe.extension.youtube.shared.RootView;
import app.morphe.extension.youtube.shared.VideoInformation;

@SuppressWarnings("unused")
public class VideoUtils extends IntentUtils {
    /**
     * Scale used to convert user speed to {@link android.widget.ProgressBar#setProgress(int)}.
     */
    private static final float PROGRESS_BAR_VALUE_SCALE = 100;
    /**
     * Formats speeds to UI strings.
     */
    private static final NumberFormat speedFormatter = NumberFormat.getNumberInstance();

    private static final String CHANNEL_URL = "https://www.youtube.com/channel/";
    private static final String PLAYLIST_URL = "https://www.youtube.com/playlist?list=";
    public static final String VIDEO_URL = "https://youtu.be/";
    private static final String VIDEO_SCHEME_INTENT_FORMAT = "vnd.youtube://%s?start=%d";
    private static final String VIDEO_SCHEME_LINK_FORMAT = "https://youtu.be/%s?t=%d";
    private static final String DEFAULT_YOUTUBE_VIDEO_QUALITY_STRING = getString("quality_auto");
    private static final AtomicBoolean isExternalDownloaderLaunched = new AtomicBoolean(false);
    private static volatile String qualityString = DEFAULT_YOUTUBE_VIDEO_QUALITY_STRING;

    static {
        // Cap at 2 decimals (rounds automatically).
        speedFormatter.setMinimumFractionDigits(2);
        speedFormatter.setMaximumFractionDigits(2);
    }

    private static String getChannelUrl(String channelId) {
        return CHANNEL_URL + channelId;
    }

    private static String getPlaylistUrl(String playlistId) {
        return PLAYLIST_URL + playlistId;
    }

    private static String getVideoUrl(String videoId) {
        return getVideoUrl(videoId, false);
    }

    public static String getVideoUrl(boolean withTimestamp) {
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

    public static String getVideoScheme(String videoId, boolean isShorts) {
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
            final String downloaderPackageName = Settings.EXTERNAL_DOWNLOADER_PACKAGE_NAME_VIDEO.get();
            // If the package is not installed, show a dialog.
            if (ExternalDownloaderVideoPreference.showDialogIfAppIsNotInstalled(RootView.getContext(), downloaderPackageName)) {
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
            final String downloaderPackageName = Settings.EXTERNAL_DOWNLOADER_PACKAGE_NAME_VIDEO_LONG_PRESS.get();
            // If the package is not installed, show a dialog.
            if (ExternalDownloaderVideoLongPressPreference.showDialogIfAppIsNotInstalled(RootView.getContext(), downloaderPackageName)) {
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
            final String downloaderPackageName = Settings.EXTERNAL_DOWNLOADER_PACKAGE_NAME_PLAYLIST.get();
            // If the package is not installed, show a dialog.
            if (ExternalDownloaderPlaylistPreference.showDialogIfAppIsNotInstalled(RootView.getContext(), downloaderPackageName)) {
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

    public static void openPlaylist(@NonNull String playlistId) {
        openPlaylist(playlistId, "");
    }

    public static void openPlaylist(@NonNull String playlistId, @NonNull String videoId) {
        openPlaylist(playlistId, videoId, false);
    }

    public static void openPlaylist(@NonNull String playlistId, @NonNull String videoId, boolean withTimestamp) {
        final StringBuilder sb = new StringBuilder();
        if (videoId.isEmpty()) {
            sb.append(getPlaylistUrl(playlistId));
        } else {
            sb.append(VIDEO_URL);
            sb.append(videoId);
            sb.append("?list=");
            sb.append(playlistId);
            if (withTimestamp) {
                final long currentVideoTimeInSeconds = VideoInformation.getVideoTimeInSeconds();
                if (currentVideoTimeInSeconds > 0) {
                    sb.append("&t=");
                    sb.append(currentVideoTimeInSeconds);
                }
            }
        }
        launchView(sb.toString(), getContext().getPackageName());
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
    public static void pauseMedia() {
        Context mContext = getContext();
        if (mContext != null && mContext.getApplicationContext().getSystemService(Context.AUDIO_SERVICE) instanceof AudioManager audioManager) {
            audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
    }

    public static void showCustomNoThemePlaybackSpeedDialog(@NonNull Context context) {
        final String[] playbackSpeedEntries = CustomPlaybackSpeedPatch.getTrimmedEntries();
        final String[] playbackSpeedEntryValues = CustomPlaybackSpeedPatch.getTrimmedEntryValues();

        final float playbackSpeed = VideoInformation.getPlaybackSpeed();
        final int index = Arrays.binarySearch(playbackSpeedEntryValues, String.valueOf(playbackSpeed));

        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setSingleChoiceItems(playbackSpeedEntries, index, (mDialog, mIndex) -> {
                    final float selectedPlaybackSpeed = Float.parseFloat(playbackSpeedEntryValues[mIndex] + "f");
                    VideoInformation.setPlaybackSpeed(selectedPlaybackSpeed);
                    VideoInformation.overridePlaybackSpeed(selectedPlaybackSpeed);
                    userSelectedPlaybackSpeed(selectedPlaybackSpeed);
                    mDialog.dismiss();
                });

        setAlertDialogThemeAndShow(builder);
    }

    public static void showCustomLegacyPlaybackSpeedDialog(@NonNull Context context) {
        final String[] playbackSpeedEntries = CustomPlaybackSpeedPatch.getTrimmedEntries();
        final String[] playbackSpeedEntryValues = CustomPlaybackSpeedPatch.getTrimmedEntryValues();

        final float playbackSpeed = VideoInformation.getPlaybackSpeed();
        final int selectedIndex = Arrays.binarySearch(playbackSpeedEntryValues, String.valueOf(playbackSpeed));

        LinearLayout mainLayout = ExtendedUtils.prepareMainLayout(context);
        Map<LinearLayout, Runnable> actionsMap = new LinkedHashMap<>(playbackSpeedEntryValues.length);
        int checkIconId = ResourceUtils.getDrawableIdentifier("quantum_ic_check_white_24");

        int i = 0;
        for (String entryValue : playbackSpeedEntryValues) {
            final float selectedPlaybackSpeed = Float.parseFloat(playbackSpeedEntryValues[i] + "f");
            Runnable action = () -> {
                VideoInformation.setPlaybackSpeed(selectedPlaybackSpeed);
                VideoInformation.overridePlaybackSpeed(selectedPlaybackSpeed);
                userSelectedPlaybackSpeed(selectedPlaybackSpeed);
            };
            LinearLayout itemLayout =
                    ExtendedUtils.createItemLayout(context, playbackSpeedEntries[i], selectedIndex == i ? checkIconId : 0);
            actionsMap.putIfAbsent(itemLayout, action);
            mainLayout.addView(itemLayout);
            i++;
        }

        ExtendedUtils.showBottomSheetDialog(context, mainLayout, actionsMap);
    }

    public static void showPlaybackSpeedDialog(@NonNull Context context,
                                               EnumSetting<PlaybackSpeedMenuType> type) {
        switch (type.get()) {
            case YOUTUBE_LEGACY -> showYouTubeLegacyPlaybackSpeedFlyoutMenu();
            case CUSTOM_NO_THEME -> showCustomNoThemePlaybackSpeedDialog(context);
            case CUSTOM_LEGACY -> showCustomLegacyPlaybackSpeedDialog(context);
            case CUSTOM_MODERN -> showCustomModernPlaybackSpeedDialog(context);
        }
    }

    public static void showShortsRepeatDialog(@NonNull Context context) {
        final EnumSetting<ShortsLoopBehavior> setting = Settings.CHANGE_SHORTS_REPEAT_STATE;
        final String settingsKey = setting.key;

        final String entryKey = settingsKey + "_entries";
        final String entryValueKey = settingsKey + "_entry_values";
        final String[] mEntries = getStringArray(entryKey);
        final String[] mEntryValues = getStringArray(entryValueKey);

        LinearLayout mainLayout = ExtendedUtils.prepareMainLayout(context);
        Map<LinearLayout, Runnable> actionsMap = new LinkedHashMap<>(mEntryValues.length);
        String currentValue = setting.get().name();
        int checkIconId = ResourceUtils.getDrawableIdentifier("quantum_ic_check_white_24");

        for (int i = 0; i < mEntryValues.length; i++) {
            String label = mEntries[i];
            String enumValue = mEntryValues[i];

            int index = i;
            Runnable action = () -> {
                for (ShortsLoopBehavior behavior : ShortsLoopBehavior.values())
                    if (behavior.ordinal() == index) setting.save(behavior);
            };
            LinearLayout itemLayout = ExtendedUtils.createItemLayout(context, label, currentValue.equals(enumValue) ? checkIconId : 0);
            actionsMap.putIfAbsent(itemLayout, action);
            mainLayout.addView(itemLayout);
        }
        ExtendedUtils.showBottomSheetDialog(context, mainLayout, actionsMap);
    }

    public static String getFormattedQualityString(@Nullable String prefix) {
        return prefix == null ? qualityString : String.format("%s\u2009•\u2009%s", prefix, qualityString);
    }

    public static void updateQualityString(@Nullable String qualityName) {
        if (StringUtils.isEmpty(qualityName)) {
            qualityString = DEFAULT_YOUTUBE_VIDEO_QUALITY_STRING;
            return;
        }
        try {
            int suffixIndex = StringUtils.indexOfAny(qualityName, "p", "s");
            if (suffixIndex > -1) {
                String videoQualityIntString = StringUtils.substring(qualityName, 0, suffixIndex);
                qualityString = videoQualityIntString + qualityName.charAt(suffixIndex);
            }
        } catch (Exception ex) {
            Logger.printException(() -> "updateQualityString failure", ex);
        }
    }

    public static String getFormattedSpeedString(@Nullable String prefix) {
        final float playbackSpeed = VideoInformation.getPlaybackSpeed();

        final String playbackSpeedString = isRightToLeftLocale()
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

    public static void reloadVideo() {
        if (!VideoInformation.lastPlayerResponseIsShort()) {
            String id = VideoInformation.getPlayerResponseVideoId();
            if (!id.isEmpty()) {
                reloadVideo(id);
                return;
            }
        }
        reloadVideo(VideoInformation.getVideoId());
    }

    public static void reloadVideo(@NonNull String videoId) {
        reloadVideo(videoId, VideoInformation.getPlaylistId());
    }

    /**
     * Available only when dismissPlayerHookPatch is included.
     * If the player is not active, the layout may break.
     * Use it only when it is guaranteed to be used in situations where the player is active.
     */
    public static void reloadVideo(@NonNull String videoId, @NonNull String playlistId) {
        if (videoId.isEmpty()) {
            showToastShort(str("revanced_dismiss_player_not_available_toast"));
        } else {
            try {
                dismissPlayer();

                Utils.runOnMainThreadDelayed(() -> {
                    // Open the video.
                    if (playlistId.isEmpty()) {
                        openVideo(videoId);
                    } else { // If the video is playing from a playlist, the url must include the playlistId.
                        openPlaylist(playlistId, videoId, true);
                    }
                }, 500);
            } catch (Exception ex) {
                Logger.printException(() -> "reloadVideo failed", ex);
            }
        }
    }

    /**
     * Rest of the implementation added by patch.
     */
    public static void dismissPlayer() {
        Logger.printDebug(() -> "Dismiss player");
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
    public static void showYouTubeLegacyPlaybackSpeedFlyoutMenu() {
        Logger.printDebug(() -> "Playback speed flyout menu opened");
    }

    /**
     * Rest of the implementation added by patch.
     */
    public static void showYouTubeLegacyVideoQualityFlyoutMenu() {
        // These instructions are ignored by patch.
        Log.d("Extended: VideoUtils", "Video quality flyout menu opened");
    }

    public static void showCustomVideoQualityFlyoutMenu(Context context) {
        try {
            VideoQuality[] currentQualities = VideoQualityPatch.getCurrentQualities();
            VideoQuality currentQuality = VideoQualityPatch.getCurrentQuality();
            if (currentQualities == null || currentQuality == null) {
                Logger.printDebug(() -> "Cannot show qualities dialog, videoQualities is null");
                return;
            }
            if (currentQualities.length < 2) {
                // Should never happen.
                Logger.printException(() -> "Cannot show qualities dialog, no qualities available");
                return;
            }
            VideoQualityMenuInterface menu = VideoQualityPatch.getCurrentMenuInterface();
            if (menu == null) {
                Logger.printDebug(() -> "Cannot show qualities dialog, menu is null");
                return;
            }

            // -1 adjustment for automatic quality at first index.
            int listViewSelectedIndex = -1;
            for (VideoQuality quality : currentQualities) {
                if (quality.patch_getQualityName().equals(currentQuality.patch_getQualityName())) {
                    break;
                }
                listViewSelectedIndex++;
            }

            List<String> qualityLabels = new ArrayList<>(currentQualities.length - 1);
            for (VideoQuality availableQuality : currentQualities) {
                if (availableQuality.patch_getResolution() != AUTOMATIC_VIDEO_QUALITY_VALUE) {
                    qualityLabels.add(availableQuality.patch_getQualityName());
                }
            }

            Dialog dialog = new Dialog(context);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setCanceledOnTouchOutside(true);
            dialog.setCancelable(true);

            final int dip4 = dipToPixels(4);   // Height for handle bar.
            final int dip5 = dipToPixels(5);   // Padding for mainLayout.
            final int dip6 = dipToPixels(6);   // Bottom margin.
            final int dip8 = dipToPixels(8);   // Side padding.
            final int dip16 = dipToPixels(16); // Left padding for ListView.
            final int dip20 = dipToPixels(20); // Margin below handle.
            final int dip40 = dipToPixels(40); // Width for handle bar.

            LinearLayout mainLayout = new LinearLayout(context);
            mainLayout.setOrientation(LinearLayout.VERTICAL);
            mainLayout.setPadding(dip5, dip8, dip5, dip8);

            ShapeDrawable background = new ShapeDrawable(new RoundRectShape(
                    Utils.createCornerRadii(12), null, null));
            background.getPaint().setColor(BaseThemeUtils.getDialogBackgroundColor());
            mainLayout.setBackground(background);

            View handleBar = new View(context);
            ShapeDrawable handleBackground = new ShapeDrawable(new RoundRectShape(
                    Utils.createCornerRadii(4), null, null));
            final int baseColor = BaseThemeUtils.getDialogBackgroundColor();
            final int adjustedHandleBarBackgroundColor = ThemeUtils.adjustColorBrightness(
                    baseColor, 0.9f, 1.25f);
            handleBackground.getPaint().setColor(adjustedHandleBarBackgroundColor);
            handleBar.setBackground(handleBackground);
            LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams(dip40, dip4);
            handleParams.gravity = Gravity.CENTER_HORIZONTAL;
            handleParams.setMargins(0, 0, 0, dip20);
            handleBar.setLayoutParams(handleParams);
            mainLayout.addView(handleBar);

            if (!Settings.HIDE_PLAYER_FLYOUT_MENU_QUALITY_HEADER.get()) {
                // Create SpannableStringBuilder for formatted text.
                SpannableStringBuilder spannableTitle = new SpannableStringBuilder();
                String titlePart = str("video_quality_quick_menu_title");
                String separatorPart = str("video_quality_title_seperator");

                // Append title part with default foreground color.
                spannableTitle.append(titlePart);
                spannableTitle.setSpan(
                        new ForegroundColorSpan(ThemeUtils.getAppForegroundColor()),
                        0,
                        titlePart.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                spannableTitle.append("   "); // Space after title.

                // Append separator part with adjusted title color.
                int separatorStart = spannableTitle.length();
                spannableTitle.append(separatorPart);
                final int adjustedTitleForegroundColor = ThemeUtils.adjustColorBrightness(
                        ThemeUtils.getAppForegroundColor(), 1.6f, 0.6f);
                spannableTitle.setSpan(
                        new ForegroundColorSpan(adjustedTitleForegroundColor),
                        separatorStart,
                        separatorStart + separatorPart.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                spannableTitle.append("   "); // Space after separator.

                // Append quality label with adjusted title color.
                final int qualityStart = spannableTitle.length();
                spannableTitle.append(currentQuality.patch_getQualityName());
                spannableTitle.setSpan(
                        new ForegroundColorSpan(adjustedTitleForegroundColor),
                        qualityStart,
                        qualityStart + currentQuality.patch_getQualityName().length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );

                // Add title with current quality.
                TextView titleView = new TextView(context);
                titleView.setText(spannableTitle);
                titleView.setTextSize(16);
                // Remove setTextColor since color is handled by SpannableStringBuilder.
                LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                titleParams.setMargins(dip8, 0, 0, dip20);
                titleView.setLayoutParams(titleParams);
                mainLayout.addView(titleView);
            }

            ListView listView = new ListView(context);
            ExtendedUtils.CustomAdapter adapter = new ExtendedUtils.CustomAdapter(context, qualityLabels);
            adapter.setSelectedPosition(listViewSelectedIndex);
            listView.setAdapter(adapter);
            listView.setDivider(null);
            listView.setPadding(dip16, 0, 0, 0);

            listView.setOnItemClickListener((parent, view, which, id) -> {
                try {
                    final int originalIndex = which + 1; // Adjust for automatic.
                    VideoQuality selectedQuality = currentQualities[originalIndex];
                    Logger.printDebug(() -> "User clicked on quality: " + selectedQuality);

                    if (VideoQualityPatch.shouldRememberVideoQuality()) {
                        VideoQualityPatch.saveDefaultQuality(selectedQuality.patch_getResolution());
                    }
                    // Don't update button icon now. Icon will update when the actual
                    // quality is changed by YT.  This is needed to ensure the icon is correct
                    // if YT ignores changing from 1080p Premium to regular 1080p.
                    menu.patch_setQuality(selectedQuality);
                    setCurrentQuality(selectedQuality);

                    dialog.dismiss();
                } catch (Exception ex) {
                    Logger.printException(() -> "Video quality selection failure", ex);
                }
            });

            LinearLayout.LayoutParams listViewParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            listViewParams.setMargins(0, 0, 0, dip5);
            listView.setLayoutParams(listViewParams);
            mainLayout.addView(listView);

            LinearLayout wrapperLayout = new LinearLayout(context);
            wrapperLayout.setOrientation(LinearLayout.VERTICAL);
            wrapperLayout.setPadding(dip8, 0, dip8, 0);
            wrapperLayout.addView(mainLayout);
            dialog.setContentView(wrapperLayout);

            Window window = dialog.getWindow();
            if (window != null) {
                WindowManager.LayoutParams params = window.getAttributes();
                params.gravity = Gravity.BOTTOM;
                params.y = dip6;
                int portraitWidth = context.getResources().getDisplayMetrics().widthPixels;
                if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    portraitWidth = Math.min(
                            portraitWidth,
                            context.getResources().getDisplayMetrics().heightPixels);
                }
                params.width = portraitWidth;
                params.height = WindowManager.LayoutParams.WRAP_CONTENT;
                window.setAttributes(params);
                window.setBackgroundDrawable(null);
            }

            final int fadeDurationFast = ResourceUtils.getInteger("fade_duration_fast");
            Animation slideInABottomAnimation = ResourceUtils.getAnimation("slide_in_bottom");
            if (slideInABottomAnimation != null) {
                slideInABottomAnimation.setDuration(fadeDurationFast);
            }
            mainLayout.startAnimation(slideInABottomAnimation);

            // noinspection ClickableViewAccessibility
            mainLayout.setOnTouchListener(new View.OnTouchListener() {
                final float dismissThreshold = dipToPixels(100);
                float touchY;
                float translationY;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            touchY = event.getRawY();
                            translationY = mainLayout.getTranslationY();
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            final float deltaY = event.getRawY() - touchY;
                            if (deltaY >= 0) {
                                mainLayout.setTranslationY(translationY + deltaY);
                            }
                            return true;
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            if (mainLayout.getTranslationY() > dismissThreshold) {
                                //noinspection ExtractMethodRecommender
                                final float remainingDistance = context.getResources().getDisplayMetrics().heightPixels
                                        - mainLayout.getTop();
                                TranslateAnimation slideOut = new TranslateAnimation(
                                        0, 0, mainLayout.getTranslationY(), remainingDistance);
                                slideOut.setDuration(fadeDurationFast);
                                slideOut.setAnimationListener(new Animation.AnimationListener() {
                                    @Override
                                    public void onAnimationStart(Animation animation) {
                                    }

                                    @Override
                                    public void onAnimationEnd(Animation animation) {
                                        dialog.dismiss();
                                    }

                                    @Override
                                    public void onAnimationRepeat(Animation animation) {
                                    }
                                });
                                mainLayout.startAnimation(slideOut);
                            } else {
                                TranslateAnimation slideBack = new TranslateAnimation(
                                        0, 0, mainLayout.getTranslationY(), 0);
                                slideBack.setDuration(fadeDurationFast);
                                mainLayout.startAnimation(slideBack);
                                mainLayout.setTranslationY(0);
                            }
                            return true;
                        default:
                            return false;
                    }
                }
            });

            dialog.show();
            return;
        } catch (Exception ex) {
            Logger.printException(() -> "showCustomVideoQualityFlyoutMenu failure", ex);
        }
        showYouTubeLegacyVideoQualityFlyoutMenu();
    }

    /**
     * Displays a modern custom dialog for adjusting video playback speed.
     * <p>
     * This method creates a dialog with a slider, plus/minus buttons, and preset speed buttons
     * to allow the user to modify the video playback speed. The dialog is styled with rounded
     * corners and themed colors, positioned at the bottom of the screen. The playback speed
     * can be adjusted in 0.05 increments using the slider or buttons, or set directly to preset
     * values. The dialog updates the displayed speed in real-time and applies changes to the
     * video playback. The dialog is dismissed if the player enters Picture-in-Picture (PiP) mode.
     */
    public static void showCustomModernPlaybackSpeedDialog(Context context) {
        try {
            // Create main layout.
            SheetBottomDialog.DraggableLinearLayout mainLayout =
                    SheetBottomDialog.createMainLayout(context, getDialogBackgroundColor());

            // Preset size constants.
            final int dip4 = dipToPixels(4);
            final int dip8 = dipToPixels(8);
            final int dip12 = dipToPixels(12);
            final int dip20 = dipToPixels(20);
            final int dip32 = dipToPixels(32);
            final int dip60 = dipToPixels(60);

            // Display current playback speed.
            TextView currentSpeedText = new TextView(context);
            float currentSpeed = VideoInformation.getPlaybackSpeed();
            // Initially show with only 0 minimum digits, so 1.0 shows as 1x.
            currentSpeedText.setText(formatSpeedStringX(currentSpeed));
            currentSpeedText.setTextColor(ThemeUtils.getAppForegroundColor());
            currentSpeedText.setTextSize(16);
            currentSpeedText.setTypeface(Typeface.DEFAULT_BOLD);
            currentSpeedText.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            textParams.setMargins(0, dip20, 0, 0);
            currentSpeedText.setLayoutParams(textParams);
            // Add current speed text view to main layout.
            mainLayout.addView(currentSpeedText);

            // Create horizontal layout for slider and +/- buttons.
            LinearLayout sliderLayout = new LinearLayout(context);
            sliderLayout.setOrientation(LinearLayout.HORIZONTAL);
            sliderLayout.setGravity(Gravity.CENTER_VERTICAL);

            // Create +/- buttons.
            Button minusButton = createStyledButton(context, false, dip8, dip8);
            Button plusButton = createStyledButton(context, true, dip8, dip8);

            // Create slider for speed adjustment.
            SeekBar speedSlider = new SeekBar(context);
            speedSlider.setFocusable(true);
            speedSlider.setFocusableInTouchMode(true);
            speedSlider.setMax(speedToProgressValue(CustomPlaybackSpeedPatch.getPlaybackSpeedMaximum()));
            speedSlider.setProgress(speedToProgressValue(currentSpeed));
            speedSlider.getProgressDrawable().setColorFilter(
                    ThemeUtils.getAppForegroundColor(), PorterDuff.Mode.SRC_IN); // Theme progress bar.
            speedSlider.getThumb().setColorFilter(
                    ThemeUtils.getAppForegroundColor(), PorterDuff.Mode.SRC_IN); // Theme slider thumb.
            LinearLayout.LayoutParams sliderParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            speedSlider.setLayoutParams(sliderParams);

            // Add -/+ and slider views to slider layout.
            sliderLayout.addView(minusButton);
            sliderLayout.addView(speedSlider);
            sliderLayout.addView(plusButton);

            // Add slider layout to main layout.
            mainLayout.addView(sliderLayout);

            Function<Float, Void> userSelectedSpeed = newSpeed -> {
                final float roundedSpeed = roundSpeedToNearestIncrement(newSpeed);
                if (VideoInformation.getPlaybackSpeed() == roundedSpeed) {
                    // Nothing has changed. New speed rounds to the current speed.
                    return null;
                }

                VideoInformation.overridePlaybackSpeed(roundedSpeed);
                PlaybackSpeedPatch.userSelectedPlaybackSpeed(roundedSpeed);
                currentSpeedText.setText(formatSpeedStringX(roundedSpeed, 2)); // Update display.
                speedSlider.setProgress(speedToProgressValue(roundedSpeed)); // Update slider.
                return null;
            };

            // Set listener for slider to update playback speed.
            speedSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        // Convert from progress value to video playback speed.
                        userSelectedSpeed.apply(CustomPlaybackSpeedPatch.getPlaybackSpeedMinimum() + (progress / PROGRESS_BAR_VALUE_SCALE));
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });

            minusButton.setOnClickListener(v -> userSelectedSpeed.apply(
                    (float) (VideoInformation.getPlaybackSpeed() - SPEED_ADJUSTMENT_CHANGE)));
            plusButton.setOnClickListener(v -> userSelectedSpeed.apply(
                    (float) (VideoInformation.getPlaybackSpeed() + SPEED_ADJUSTMENT_CHANGE)));

            // Create GridLayout for preset speed buttons.
            GridLayout gridLayout = new GridLayout(context);
            gridLayout.setColumnCount(5); // 5 columns for speed buttons.
            gridLayout.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
            gridLayout.setRowCount((int) Math.ceil(CustomPlaybackSpeedPatch.getLength() / 5.0));
            LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            gridParams.setMargins(dip4, dip12, dip4, dip12); // Speed buttons container.
            gridLayout.setLayoutParams(gridParams);

            // For button use 1 digit minimum.
            speedFormatter.setMinimumFractionDigits(1);

            // Add buttons for each preset playback speed.
            for (float speed : CustomPlaybackSpeedPatch.getPlaybackSpeeds()) {
                // Container for button and optional label.
                FrameLayout buttonContainer = new FrameLayout(context);

                // Set layout parameters for each grid cell.
                GridLayout.LayoutParams containerParams = new GridLayout.LayoutParams();
                containerParams.width = 0; // Equal width for columns.
                containerParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
                containerParams.setMargins(dip4, 0, dip4, 0); // Button margins.
                containerParams.height = dip60; // Fixed height for button and label.
                buttonContainer.setLayoutParams(containerParams);

                // Create speed button.
                Button speedButton = new Button(context, null, 0);
                speedButton.setText(speedFormatter.format(speed));
                speedButton.setTextColor(ThemeUtils.getAppForegroundColor());
                speedButton.setTextSize(12);
                speedButton.setAllCaps(false);
                speedButton.setGravity(Gravity.CENTER);

                ShapeDrawable buttonBackground = new ShapeDrawable(new RoundRectShape(
                        Utils.createCornerRadii(20), null, null));
                buttonBackground.getPaint().setColor(getAdjustedBackgroundColor(false));
                speedButton.setBackground(buttonBackground);
                speedButton.setPadding(dip4, dip4, dip4, dip4);

                // Center button vertically and stretch horizontally in container.
                FrameLayout.LayoutParams buttonParams = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, dip32, Gravity.CENTER);
                speedButton.setLayoutParams(buttonParams);

                // Add speed buttons view to buttons container layout.
                buttonContainer.addView(speedButton);

                // Add "Normal" label for 1.0x speed.
                if (speed == 1.0f) {
                    TextView normalLabel = new TextView(context);
                    // Use same 'Normal' string as stock YouTube.
                    normalLabel.setText(str("revanced_playback_speed_normal"));
                    normalLabel.setTextColor(ThemeUtils.getAppForegroundColor());
                    normalLabel.setTextSize(10);
                    normalLabel.setGravity(Gravity.CENTER);

                    FrameLayout.LayoutParams labelParams = new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                            Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
                    labelParams.bottomMargin = 0; // Position label below button.
                    normalLabel.setLayoutParams(labelParams);

                    buttonContainer.addView(normalLabel);
                }

                speedButton.setOnClickListener(v -> userSelectedSpeed.apply(speed));

                gridLayout.addView(buttonContainer);
            }

            // Restore 2 digit minimum.
            speedFormatter.setMinimumFractionDigits(2);

            // Add in-rows speed buttons layout to main layout.
            mainLayout.addView(gridLayout);

            ExtendedUtils.showBottomSheetDialog(context, mainLayout);
        } catch (Exception ex) {
            Logger.printException(() -> "showCustomModernPlaybackSpeedDialog failure", ex);
        }
    }

    /**
     * Creates a styled button with a plus or minus symbol.
     *
     * @param context     The Android context used to create the button.
     * @param isPlus      True to display a plus symbol, false to display a minus symbol.
     * @param marginStart The start margin in pixels (left for LTR, right for RTL).
     * @param marginEnd   The end margin in pixels (right for LTR, left for RTL).
     * @return A configured {@link Button} with the specified styling and layout parameters.
     */
    private static Button createStyledButton(Context context, boolean isPlus, int marginStart, int marginEnd) {
        Button button = new Button(context, null, 0); // Disable default theme style.
        button.setText(""); // No text on button.
        ShapeDrawable background = new ShapeDrawable(new RoundRectShape(
                Utils.createCornerRadii(20), null, null));
        background.getPaint().setColor(getAdjustedBackgroundColor(false));
        button.setBackground(background);
        button.setForeground(new OutlineSymbolDrawable(isPlus)); // Plus or minus symbol.
        final int dip36 = dipToPixels(36);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dip36, dip36);
        params.setMargins(marginStart, 0, marginEnd, 0); // Set margins.
        button.setLayoutParams(params);
        return button;
    }

    /**
     * @param speed The playback speed value to format.
     * @return A string representation of the speed with 'x' (e.g. "1.25x" or "1.00x").
     */
    private static String formatSpeedStringX(float speed) {
        return speedFormatter.format(speed) + 'x';
    }

    /**
     * @param speed The playback speed value to format.
     * @return A string representation of the speed with 'x' (e.g. "1.25x" or "1.00x").
     */
    public static String formatSpeedStringX(float speed, int minimumFractionDigits) {
        speedFormatter.setMinimumFractionDigits(minimumFractionDigits);
        return speedFormatter.format(speed) + 'x';
    }

    /**
     * @return user speed converted to a value for {@link SeekBar#setProgress(int)}.
     */
    private static int speedToProgressValue(float speed) {
        return (int) ((speed - CustomPlaybackSpeedPatch.getPlaybackSpeedMinimum()) * PROGRESS_BAR_VALUE_SCALE);
    }

    /**
     * Rounds the given playback speed to the nearest 0.05 increment,
     * unless the speed exactly matches a preset custom speed.
     *
     * @param speed The playback speed to round.
     * @return The rounded speed, constrained to the specified bounds.
     */
    private static float roundSpeedToNearestIncrement(float speed) {
        // Allow speed as-is if it exactly matches a speed preset such as 1.03x.
        if (ArrayUtils.contains(CustomPlaybackSpeedPatch.getArray(), speed)) {
            return speed;
        }

        // Round to nearest 0.05 speed.  Must use double precision otherwise rounding error can occur.
        final double roundedSpeed = Math.round(speed / SPEED_ADJUSTMENT_CHANGE) * SPEED_ADJUSTMENT_CHANGE;
        return Utils.clamp((float) roundedSpeed, (float) SPEED_ADJUSTMENT_CHANGE, PLAYBACK_SPEED_MAXIMUM);
    }
}

/**
 * Custom Drawable for rendering outlined plus and minus symbols on buttons.
 */
class OutlineSymbolDrawable extends Drawable {
    private final boolean isPlus; // Determines if the symbol is a plus or minus.
    private final Paint paint;

    OutlineSymbolDrawable(boolean isPlus) {
        this.isPlus = isPlus;
        paint = new Paint(Paint.ANTI_ALIAS_FLAG); // Enable anti-aliasing for smooth rendering.
        paint.setColor(ThemeUtils.getAppForegroundColor());
        paint.setStyle(Paint.Style.STROKE); // Use stroke style for outline.
        paint.setStrokeWidth(dipToPixels(1)); // 1dp stroke width.
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        final int width = bounds.width();
        final int height = bounds.height();
        final float centerX = width / 2f; // Center X coordinate.
        final float centerY = height / 2f; // Center Y coordinate.
        final float size = Math.min(width, height) * 0.25f; // Symbol size is 25% of button dimensions.

        // Draw horizontal line for both plus and minus symbols.
        canvas.drawLine(centerX - size, centerY, centerX + size, centerY, paint);
        if (isPlus) {
            // Draw vertical line for plus symbol.
            canvas.drawLine(centerX, centerY - size, centerX, centerY + size, paint);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
