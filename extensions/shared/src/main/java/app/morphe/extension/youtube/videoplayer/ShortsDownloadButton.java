/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/4099
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.extension.youtube.videoplayer;

import static app.morphe.extension.shared.utils.StringRef.str;

import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.regex.Pattern;

import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.ResourceUtils;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.shared.patches.components.BufferAsciiStrings;
import app.morphe.extension.shared.settings.BooleanSetting;
import static app.morphe.extension.youtube.settings.YouTubeActivityHook.USE_BOLD_ICONS;
import app.morphe.extension.youtube.utils.VideoUtils;
import app.morphe.extension.youtube.patches.general.DownloadActionsPatch;
import app.morphe.extension.youtube.patches.feed.FeedPatch;
import app.morphe.extension.youtube.patches.utils.FlyoutUtils;
import app.morphe.extension.youtube.settings.Settings;

/**
 * Adds a download item to the menus of Shorts, which the app has no download option for,
 * and to the menus of videos that have no download item, such as videos only Premium members can download.
 * The item downloads with the download mode that is set, with an external downloader or in-app.
 */
@SuppressWarnings("unused")
public final class ShortsDownloadButton {

    private static final boolean USE_EXPERIMENTAL_ICONS = USE_BOLD_ICONS;

    static final String DOWNLOAD_ICON = USE_EXPERIMENTAL_ICONS
            ? "yt_outline_experimental_download_vd_theme_24"
            : "yt_outline_download_black_24";

    /**
     * Duration of a video, such as 6:01 or 1:02:03, at the start of a text of the card.
     */
    private static final Pattern DURATION_PATTERN = Pattern.compile("(?:^|\u2759)\\d{1,2}(?::[0-5]\\d){1,2}");

    /**
     * Time for the menu to close, so a menu or dialog opened by the download is not dismissed with it.
     */
    private static final long DOWNLOAD_DELAY_MILLISECONDS = 100;

    @Nullable
    private static Drawable downloadIcon;

    private ShortsDownloadButton() {
    }

    @SuppressWarnings("SameReturnValue")
    public static boolean isPatchIncluded() {
        return false; // Modified during patching.
    }

    /**
     * Called by {@link FlyoutUtils} for the menu of the Shorts player and the menus of videos and Shorts in the feed.
     *
     * Shorts menus are skipped because custom Shorts actions already provide an external downloader.
     *
     * @param videoId    The video of the menu, or an empty string if unknown.
     * @param isShort    If the menu is of a Short.
     * @param menuBuffer The buffer of the card the menu belongs to, if the menu has one.
     */
    public static int addFlyoutButton(Object flyoutPanel, int nextButtonIndex, String videoId, boolean isShort,
                                      @Nullable byte[] menuBuffer) {
        try {
            // Shorts already expose the external downloader through custom Shorts actions.
            if (isShort || videoId.isEmpty()) {
                return nextButtonIndex;
            }
            BooleanSetting setting = Settings.FEED_DOWNLOAD_FLYOUT_BUTTON;
            if (!setting.get() || !setting.isAvailable()
                    // Only if the menu has no download item of the app, otherwise there are two items
                    // for the same action. The menus of playlists have no video to download.
                    || FlyoutUtils.hasAppDownloadButton() || !DownloadActionsPatch.getFlyoutPlaylistId().isEmpty()
                    // Live streams and upcoming videos cannot be downloaded,
                    // and unlike the other videos their card has no duration.
                    || menuBuffer == null
                    || !DURATION_PATTERN.matcher(new BufferAsciiStrings(menuBuffer).getStrings()).find()) {
                return nextButtonIndex;
            }

            String text = str("morphe_shorts_download_flyout_title");
            // The menus leave out the download item of the app when its text is hidden
            // by "Hide feed flyout menu", so this item is left out as well.
            if (Settings.HIDE_FEED_FLYOUT_MENU.get()
                    && (FeedPatch.hideFlyoutMenu(text) == null
                    || FeedPatch.hideFlyoutMenu(ResourceUtils.getString("menu_offline_placeholder")) == null)) {
                return nextButtonIndex;
            }

            if (downloadIcon == null) {
                downloadIcon = ResourceUtils.getDrawable(DOWNLOAD_ICON);
            }

            View.OnClickListener onClick = v -> {
                FlyoutUtils.dismissFlyout();
                Utils.runOnMainThreadDelayed(
                        () -> VideoUtils.launchVideoExternalDownloader(videoId),
                        DOWNLOAD_DELAY_MILLISECONDS
                );
            };
            // The app can bind its download item after this item is added.
            return FlyoutUtils.addFlyoutSaveButton(flyoutPanel, downloadIcon, text, onClick, nextButtonIndex, true);
        } catch (Exception ex) {
            Logger.printException(() -> "addFlyoutButton failure", ex);
            return nextButtonIndex;
        }
    }
}
