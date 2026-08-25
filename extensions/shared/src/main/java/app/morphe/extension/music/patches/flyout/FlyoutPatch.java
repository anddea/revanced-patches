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
import java.nio.charset.StandardCharsets;
import java.util.Map;

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

    private static volatile String cachedFlyoutVideoId = "";
    private static volatile long lastFlyoutDownloadTime;
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

    private static void launchExternalDownloader() {
        launchExternalDownloader(VideoInformation.getVideoId());
    }

    private static void launchExternalDownloader(String videoId) {
        cachedFlyoutVideoId = "";
        VideoUtils.launchExternalDownloader(videoId);
    }

    /**
     * Scans the raw command protobuf for an 11-byte YouTube video ID field.
     */
    @Nullable
    private static String extractVideoIdFromCommand(ProtocolBufferFieldInterface commandObj) {
        byte[] bytes = commandObj.toByteArray();
        if (bytes == null) {
            return null;
        }

        for (int i = 1, lastIndex = bytes.length - 11; i < lastIndex; i++) {
            if (bytes[i] == 11 && (bytes[i - 1] & 0b00000111) == 2) {
                if (isLikelyVideoId(bytes, i + 1) && !isBlacklisted(bytes, i + 1)) {
                    return new String(bytes, i + 1, 11, StandardCharsets.US_ASCII);
                }
            }
        }
        return null;
    }

    private static boolean isLikelyVideoId(byte[] bytes, int offset) {
        for (int i = 0; i < 11; i++) {
            byte b = bytes[offset + i];
            if (!((b >= 'a' && b <= 'z') || (b >= 'A' && b <= 'Z')
                    || (b >= '0' && b <= '9') || b == '_' || b == '-')) {
                return false;
            }
        }
        return true;
    }

    private static boolean isBlacklisted(byte[] bytes, int offset) {
        return matchesIgnoreCase(bytes, offset, "yt_") ||
                matchesIgnoreCase(bytes, offset, "video_") ||
                containsIgnoreCase(bytes, offset, 11, "download") ||
                containsIgnoreCase(bytes, offset, 11, "list_item") ||
                containsIgnoreCase(bytes, offset, 11, "button");
    }

    private static boolean matchesIgnoreCase(byte[] bytes, int offset, String target) {
        for (int i = 0, length = target.length(); i < length; i++) {
            byte b = bytes[offset + i];
            int lowerB = (b >= 'A' && b <= 'Z') ? (b + 32) : b;
            if (lowerB != target.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("SameParameterValue")
    private static boolean containsIgnoreCase(byte[] bytes, int offset, int len, String target) {
        for (int i = 0, lastIndex = len - target.length(); i <= lastIndex; i++) {
            if (matchesIgnoreCase(bytes, offset + i, target)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isViewInsideDialog(@Nullable Object viewObj) {
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
            if (!Settings.EXTERNAL_DOWNLOADER_ACTION_BUTTON.get()
                    || command == null || map == null) {
                return false;
            }
            Utils.verifyOnMainThread();

            if (ActionBarPatch.inAppDownloadButtonOnClick(map)) {
                cachedFlyoutVideoId = "";
                return true;
            }

            if (!Settings.EXTERNAL_DOWNLOADER_FLYOUT_MENU.get()) {
                return false;
            }

            String commandString = command.toString();
            final boolean isMenuOpen = commandString.contains("[98150882]");
            if (isMenuOpen) {
                String extractedId = extractVideoIdFromCommand(command);
                cachedFlyoutVideoId = extractedId == null ? "" : extractedId;
                return false;
            }

            final boolean isDownloadClick = Utils.containsAny(commandString,
                    "[133724106]", "[443434441]");
            if (isDownloadClick) {
                final long now = System.currentTimeMillis();
                if (now - lastFlyoutDownloadTime < IGNORE_DOUBLE_CLICK_DURATION_MS) {
                    return true;
                }

                Object viewObj = map.get(ELEMENTS_SENDER_VIEW);
                final boolean inDialog = isViewInsideDialog(viewObj);
                String targetId = extractVideoIdFromCommand(command);

                if (targetId == null && inDialog) {
                    targetId = cachedFlyoutVideoId;
                }

                if (targetId != null && !targetId.isEmpty()) {
                    lastFlyoutDownloadTime = now;
                    launchExternalDownloader(targetId);
                    return true;
                } else if (inDialog) {
                    lastFlyoutDownloadTime = now;
                    launchExternalDownloader();
                    return true;
                }
            }
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
