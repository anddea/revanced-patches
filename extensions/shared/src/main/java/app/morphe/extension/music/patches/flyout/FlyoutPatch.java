/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
 * - Hoàng Gia Bảo (https://github.com/YT-Advanced)
 * - inotia00 (https://github.com/inotia00)
 *
 * Licensed under the GNU General Public License v3.0.
 * Modified by lavinhoque33, 2026-10-21.
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
 * https://github.com/MorpheApp/morphe-patches/pull/1881
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.extension.music.patches.flyout;

import static app.morphe.extension.shared.utils.ResourceUtils.getIdentifier;
import static app.morphe.extension.shared.utils.StringRef.str;
import static app.morphe.extension.shared.utils.Utils.clickView;
import static app.morphe.extension.shared.utils.Utils.runOnMainThreadDelayed;

import android.app.Activity;
import android.net.Uri;
import android.util.Base64;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.regex.Pattern;

import app.morphe.extension.music.patches.actionbar.ActionBarPatch;
import app.morphe.extension.music.settings.Settings;
import app.morphe.extension.music.shared.VideoInformation;
import app.morphe.extension.music.shared.VideoType;
import app.morphe.extension.music.utils.ExtendedUtils;
import app.morphe.extension.music.utils.VideoUtils;
import app.morphe.extension.shared.settings.BooleanSetting;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.ResourceType;
import app.morphe.extension.shared.utils.Utils;

@SuppressWarnings("unused")
public class FlyoutPatch {
    /**
     * Exposes the serialized command protobuf through a stable, non-obfuscated interface.
     */
    public interface ProtocolBufferFieldInterface {
        byte[] toByteArray();
    }

    private static final BooleanSetting DISABLE_TRIM_SILENCE =
            Settings.DISABLE_TRIM_SILENCE;
    private static final BooleanSetting ENABLE_COMPACT_DIALOG =
            Settings.ENABLE_COMPACT_DIALOG;
    private static final BooleanSetting REPLACE_FLYOUT_MENU_DISMISS_QUEUE =
            Settings.REPLACE_FLYOUT_MENU_DISMISS_QUEUE;
    private static final BooleanSetting REPLACE_FLYOUT_MENU_REPORT =
            Settings.REPLACE_FLYOUT_MENU_REPORT;
    private static final BooleanSetting REPLACE_FLYOUT_MENU_REPORT_ONLY_PLAYER =
            Settings.REPLACE_FLYOUT_MENU_REPORT_ONLY_PLAYER;
    private static final boolean HIDE_FLYOUT_MENU_LIKE_DISLIKE =
            Settings.HIDE_FLYOUT_MENU_LIKE_DISLIKE.get();
    private static final String ELEMENTS_SENDER_VIEW =
            "com.google.android.libraries.youtube.rendering.elements.sender_view";
    private static final int IGNORE_DOUBLE_CLICK_DURATION_MS = 1000;

    /** Command field of the 'ypc/get_offline_upsell' endpoint. */
    private static final int OFFLINE_UPSELL_ENDPOINT = 133724106;
    private static final Pattern VIDEO_ID = Pattern.compile("[A-Za-z0-9_-]{11}");

    private static volatile long lastFlyoutDownloadTime;
    /** When the player button was last left to the app, because only menus are overridden. */
    private static volatile long lastStockPlayerButtonTime;

    /** Keeps stock player commands out of the menu hook across the split patch classes. */
    public static void onStockPlayerDownload() {
        lastStockPlayerButtonTime = System.currentTimeMillis();
    }
    private static volatile boolean lastMenuWasDismissQueue = false;
    private static WeakReference<View> touchOutSideViewRef = new WeakReference<>(null);
    private static final ColorFilter cf = new PorterDuffColorFilter(Color.parseColor("#ffffffff"), PorterDuff.Mode.SRC_ATOP);

    public static boolean disableTrimSilence(boolean original) {
        return VideoType.getCurrent().isPodCast() && !DISABLE_TRIM_SILENCE.get();
    }

    public static boolean disableTrimSilenceSwitch(boolean original) {
        return VideoType.getCurrent().isPodCast() && !DISABLE_TRIM_SILENCE.get();
    }

    public static int enableCompactDialog(int original) {
        return ENABLE_COMPACT_DIALOG.get()
                ? Math.max(original, 600)
                : original;
    }

    private static void launchExternalDownloader(String videoId) {
        VideoUtils.launchExternalDownloader(videoId);
    }

    private static boolean isFlyoutMenuOverridden() {
        return Settings.IN_APP_DOWNLOADS.get() || Settings.EXTERNAL_DOWNLOADER_FLYOUT_MENU.get();
    }

    /**
     * The player button is handled by the command resolver, so a Download command that reaches
     * the app is from a menu, unless the player button was just left to the app.
     */
    private static boolean onMenuDownload(String videoId) {
        if (!isFlyoutMenuOverridden()) return false;

        final long now = System.currentTimeMillis();
        if (now - lastStockPlayerButtonTime < IGNORE_DOUBLE_CLICK_DURATION_MS) return false;
        // The song menu stays open after Download, so repeated taps can repeat the request.
        // Keep this timer separate from the player button so a menu download of another song is not blocked.
        if (now - lastFlyoutDownloadTime < IGNORE_DOUBLE_CLICK_DURATION_MS) return true;
        lastFlyoutDownloadTime = now;

        Logger.printDebug(() -> "Menu download of " + videoId);
        launchExternalDownloader(videoId);
        return true;
    }

    /**
     * The endpoint holds the video ID, or encoded parameters with the video ID in field 4.
     *
     * @return The video ID, or an empty string for albums and playlists.
     */
    private static String getOfflineUpsellVideoId(byte[] command) {
        String value = new ProtoMessage(command).messageField(OFFLINE_UPSELL_ENDPOINT).stringField(1);
        if (value.isEmpty() || VIDEO_ID.matcher(value).matches()) return value;

        return new ProtoMessage(Base64.decode(Uri.decode(value), Base64.URL_SAFE)).stringField(4);
    }

    /**
     * Injection point.
     * Called for accounts that are allowed to save the song.
     */
    public static boolean offlineVideoEndpointOnClick(String videoId) {
        try {
            Utils.verifyOnMainThread();
            return onMenuDownload(videoId);
        } catch (Exception ex) {
            Logger.printException(() -> "offlineVideoEndpointOnClick failure", ex);
            return false;
        }
    }

    /**
     * Injection point.
     * Called for accounts that are offered Premium instead of saving the song.
     */
    public static boolean offlineUpsellOnClick(@Nullable Object command) {
        try {
            if (!isFlyoutMenuOverridden()
                    || !(command instanceof ProtocolBufferFieldInterface message)) {
                return false;
            }
            Utils.verifyOnMainThread();

            String videoId = getOfflineUpsellVideoId(message.toByteArray());
            // Albums and playlists are left to the app.
            if (videoId.isEmpty()) return false;

            return onMenuDownload(videoId);
        } catch (Exception ex) {
            Logger.printException(() -> "offlineUpsellOnClick failure", ex);
            return false;
        }
    }

    public static boolean isViewInsideDialog(@Nullable Object viewObj) {
        if (viewObj instanceof View view) {
            View buttonRoot = view.getRootView();

            Activity activity = Utils.getActivity();
            if (activity != null) {
                View activityRoot = activity.getWindow().getDecorView();
                return buttonRoot != activityRoot;
            }
        }
        return false;
    }

    /**
     * Intercepts matching Download commands from Music's flyout menu.
     */
    public static boolean commandResolverOnClick(ProtocolBufferFieldInterface command,
                                                  Map<Object, Object> map) {
        try {
            if (command == null || map == null) {
                return false;
            }
            Utils.verifyOnMainThread();

            if (ActionBarPatch.inAppDownloadButtonOnClick(map)) {
                return true;
            }

            // Menu Download commands have no sender view, and are handled when they reach
            // the offline video endpoint or the offline upsell.
        } catch (Exception ex) {
            Logger.printException(() -> "commandResolverOnClick failure", ex);
        }
        return false;
    }

    public static boolean hideComponents(@Nullable Enum<?> flyoutMenuEnum) {
        if (flyoutMenuEnum != null) {
            final String flyoutMenuName = flyoutMenuEnum.name();
            for (FlyoutPanelComponent component : FlyoutPanelComponent.values()) {
                if (component.name().equals(flyoutMenuName)) {
                    final boolean hidden = component.shouldHide();
                    Logger.printDebug(() -> "flyoutMenu loaded: " + flyoutMenuName +
                            ", hidden: " + hidden);
                    return hidden;
                }
            }
            Logger.printDebug(() -> "flyoutMenu loaded: " + flyoutMenuName + ", unmapped");
        }

        return false;
    }

    public static void hideLikeDislikeContainer(View view) {
        if (HIDE_FLYOUT_MENU_LIKE_DISLIKE &&
                view.getParent() instanceof ViewGroup viewGroup) {
            viewGroup.removeView(view);
        }
    }

    public static void setTouchOutSideView(View touchOutSideView) {
        touchOutSideViewRef = new WeakReference<>(touchOutSideView);
    }

    public static void replaceComponents(@Nullable Enum<?> flyoutPanelEnum, @NonNull TextView textView, @NonNull ImageView imageView) {
        if (flyoutPanelEnum == null)
            return;

        final String enumString = flyoutPanelEnum.name();
        final boolean isDismissQue = enumString.equals("DISMISS_QUEUE");
        final boolean isReport = enumString.equals("FLAG");

        if (isDismissQue) {
            replaceDismissQueue(textView, imageView);
        } else if (isReport) {
            replaceReport(textView, imageView, lastMenuWasDismissQueue);
        }
        lastMenuWasDismissQueue = isDismissQue;
    }

    private static void replaceDismissQueue(@NonNull TextView textView, @NonNull ImageView imageView) {
        if (REPLACE_FLYOUT_MENU_DISMISS_QUEUE.get() &&
                textView.getParent() instanceof ViewGroup clickAbleArea) {
            runOnMainThreadDelayed(() -> {
                        textView.setText(str("revanced_replace_flyout_menu_dismiss_queue_watch_on_youtube_label"));
                        final String drawableName = ExtendedUtils.IS_9_00_OR_GREATER
                                ? "yt_bold_youtube_logo_icon_vd_theme_24"
                                : "yt_outline_youtube_logo_icon_vd_theme_24";
                        imageView.setImageResource(getIdentifier(drawableName, ResourceType.DRAWABLE, clickAbleArea.getContext()));
                        clickAbleArea.setOnClickListener(view -> {
                            clickView(touchOutSideViewRef.get());
                            VideoUtils.openInYouTube();
                        });
                    }, 0L
            );
        }
    }

    private static void replaceReport(@NonNull TextView textView, @NonNull ImageView imageView,
                                      boolean wasDismissQueue) {
        if (REPLACE_FLYOUT_MENU_REPORT.get() &&
                (!REPLACE_FLYOUT_MENU_REPORT_ONLY_PLAYER.get() || wasDismissQueue) &&
                textView.getParent() instanceof ViewGroup clickAbleArea
        ) {
            runOnMainThreadDelayed(() -> {
                        textView.setText(str("playback_rate_title"));
                        final String drawableName = ExtendedUtils.IS_9_00_OR_GREATER
                                ? "yt_bold_play_arrow_half_circle_black_24"
                                : "yt_outline_play_arrow_half_circle_black_24";
                        imageView.setImageResource(getIdentifier(drawableName, ResourceType.DRAWABLE, clickAbleArea.getContext()));
                        imageView.setColorFilter(cf);
                        clickAbleArea.setOnClickListener(view -> {
                            clickView(touchOutSideViewRef.get());
                            VideoUtils.showPlaybackSpeedFlyoutMenu();
                        });
                    }, 0L
            );
        }
    }

    private enum FlyoutPanelComponent {
        ADD_TO_PLAYLIST(Settings.HIDE_FLYOUT_MENU_SAVE_TO_PLAYLIST),
        ALBUM(Settings.HIDE_FLYOUT_MENU_GO_TO_ALBUM),
        ARTIST(Settings.HIDE_FLYOUT_MENU_GO_TO_ARTIST),
        BOOKMARK(Settings.HIDE_FLYOUT_MENU_REMOVE_FROM_LIBRARY),
        BOOKMARK_BORDER(Settings.HIDE_FLYOUT_MENU_SAVE_EPISODE_FOR_LATER_SAVE_TO_LIBRARY),
        BROADCAST(Settings.HIDE_FLYOUT_MENU_GO_TO_PODCAST),
        CAPTIONS(Settings.HIDE_FLYOUT_MENU_CAPTIONS),
        DELETE(Settings.HIDE_FLYOUT_MENU_DELETE_PLAYLIST),
        DISMISS_QUEUE(Settings.HIDE_FLYOUT_MENU_DISMISS_QUEUE),
        EDIT(Settings.HIDE_FLYOUT_MENU_EDIT_PLAYLIST),
        FLAG(Settings.HIDE_FLYOUT_MENU_REPORT),
        HELP_OUTLINE(Settings.HIDE_FLYOUT_MENU_HELP),
        HIDE(Settings.HIDE_FLYOUT_MENU_NOT_INTERESTED),
        INFO(Settings.HIDE_FLYOUT_MENU_GO_TO_EPISODE),
        KEEP(Settings.HIDE_FLYOUT_MENU_PIN_TO_SPEED_DIAL),
        KEEP_OFF(Settings.HIDE_FLYOUT_MENU_UNPIN_FROM_SPEED_DIAL),
        LIBRARY_ADD(Settings.HIDE_FLYOUT_MENU_SAVE_EPISODE_FOR_LATER_SAVE_TO_LIBRARY),
        LIBRARY_REMOVE(Settings.HIDE_FLYOUT_MENU_REMOVE_FROM_LIBRARY),
        MIX(Settings.HIDE_FLYOUT_MENU_START_RADIO),
        MOON_Z(Settings.HIDE_FLYOUT_MENU_SLEEP_TIMER),
        OFFLINE_DOWNLOAD(Settings.HIDE_FLYOUT_MENU_DOWNLOAD),
        PEOPLE_GROUP(Settings.HIDE_FLYOUT_MENU_VIEW_SONG_CREDIT),
        PIN_OFF_OUTLINE(Settings.HIDE_FLYOUT_MENU_UNPIN_FROM_SPEED_DIAL),
        PIN_OUTLINE(Settings.HIDE_FLYOUT_MENU_PIN_TO_SPEED_DIAL),
        PLANNER_REVIEW(Settings.HIDE_FLYOUT_MENU_STATS_FOR_NERDS),
        QUEUE_MUSIC(Settings.HIDE_FLYOUT_MENU_ADD_TO_QUEUE),
        QUEUE_PLAY_NEXT(Settings.HIDE_FLYOUT_MENU_PLAY_NEXT),
        REMOVE_FROM_PLAYLIST(Settings.HIDE_FLYOUT_MENU_REMOVE_FROM_PLAYLIST),
        SETTINGS_MATERIAL(Settings.HIDE_FLYOUT_MENU_QUALITY),
        SHARE(Settings.HIDE_FLYOUT_MENU_SHARE),
        SHUFFLE(Settings.HIDE_FLYOUT_MENU_SHUFFLE_PLAY),
        SUBSCRIBE(Settings.HIDE_FLYOUT_MENU_SUBSCRIBE);

        private final BooleanSetting[] settings;

        FlyoutPanelComponent(BooleanSetting... settings) {
            this.settings = settings;
        }

        boolean shouldHide() {
            for (BooleanSetting setting : settings) {
                if (setting.get()) {
                    return true;
                }
            }
            return false;
        }
    }
}
