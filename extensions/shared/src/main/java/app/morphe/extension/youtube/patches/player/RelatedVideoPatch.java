/*
 * Portions of this file are ported from Morphe:
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to Morphe contributions.
 */

package app.morphe.extension.youtube.patches.player;

import com.google.protobuf.MessageLite;

import androidx.annotation.NonNull;

import java.util.LinkedHashMap;
import java.util.Map;

import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.youtube.innertube.NextResponseOuterClass.SecondaryContents;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.shared.BottomSheetState;
import app.morphe.extension.youtube.shared.EngagementPanel;
import app.morphe.extension.youtube.shared.PlayerType;
import app.morphe.extension.youtube.shared.RootView;

@SuppressWarnings("unused")
public final class RelatedVideoPatch {
    private static final String SCROLL_TO_TOP_LINEAR_LAYOUT_MANAGER_CLASS =
            "com.google.android.libraries.youtube.rendering.ui.ScrollToTopLinearLayoutManager";
    private static final boolean HIDE_RELATED_VIDEOS = Settings.HIDE_RELATED_VIDEOS.get();
    private static final int OFFSET = Settings.RELATED_VIDEOS_OFFSET.get();
    private static final int MAX_ITEM_COUNT = 4 + OFFSET;
    private static final String COMMENTS = "comments";
    private static volatile boolean isFiltered;
    private static final Map<String, Integer> lastVideoIds = new LinkedHashMap<>() {
        private static final int NUMBER_OF_LAST_VIDEO_IDS_TO_TRACK = 1;

        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > NUMBER_OF_LAST_VIDEO_IDS_TO_TRACK;
        }
    };
    private static String videoId = "";

    /**
     * Legacy injection point for YouTube versions before 20.21.
     */
    public static void newVideoStarted(@NonNull String newlyLoadedChannelId,
                                       @NonNull String newlyLoadedChannelName,
                                       @NonNull String newlyLoadedVideoId,
                                       @NonNull String newlyLoadedVideoTitle,
                                       final long newlyLoadedVideoLength,
                                       boolean newlyLoadedLiveStreamValue) {
        if (!videoId.equals(newlyLoadedVideoId) &&
                PlayerType.getCurrent() != PlayerType.WATCH_WHILE_MINIMIZED) {
            videoId = newlyLoadedVideoId;
            lastVideoIds.clear();
            Logger.printDebug(() -> "newVideoStarted: " + newlyLoadedVideoId);
        }
    }

    /**
     * Legacy player-dismiss observer for YouTube versions before 20.21.
     */
    public static void onDismiss(int index) {
        if (HIDE_RELATED_VIDEOS && index == 0) {
            videoId = "";
            lastVideoIds.clear();
        }
    }

    /**
     * Legacy item-count override retained for YouTube versions before 20.21.
     *
     * @param itemCounts original recycler item count
     * @return item count limited before related videos
     */
    public static int overrideItemCounts(int itemCounts) {
        if (!HIDE_RELATED_VIDEOS || itemCounts < MAX_ITEM_COUNT) {
            return itemCounts;
        }
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        if (elements.length < 7) {
            return itemCounts;
        }
        StackTraceElement sixthElement = elements[6];
        if (sixthElement == null ||
                !sixthElement.toString().startsWith(SCROLL_TO_TOP_LINEAR_LAYOUT_MANAGER_CLASS) ||
                videoId.isEmpty()) {
            return itemCounts;
        }
        Integer count = lastVideoIds.get(videoId);
        if (count != null && itemCounts == count &&
                PlayerType.getCurrent().isMaximizedOrFullscreenOrSliding()) {
            return MAX_ITEM_COUNT;
        }
        if (!RootView.isPlayerActive() ||
                BottomSheetState.getCurrent().isOpen() ||
                EngagementPanel.isOpen()) {
            return itemCounts;
        }
        if (count == null) {
            lastVideoIds.put(videoId, itemCounts);
            return MAX_ITEM_COUNT;
        }
        return PlayerType.getCurrent().isMaximizedOrFullscreenOrSliding() &&
                Math.abs(itemCounts - count) < 5
                ? MAX_ITEM_COUNT
                : itemCounts;
    }

    /**
     * Injection point that resets filtering state for each watch-next response.
     *
     * @return whether related videos should be hidden
     */
    public static boolean hideRelatedVideos() {
        if (HIDE_RELATED_VIDEOS) {
            isFiltered = false;
        }

        return HIDE_RELATED_VIDEOS;
    }

    /**
     * Identifies related-video item sections while preserving the comments section.
     *
     * @param messageLite watch-next secondary content
     * @return whether the item section contains related videos
     */
    public static boolean isRelatedItems(MessageLite messageLite) {
        try {
            SecondaryContents secondaryContents = SecondaryContents.parseFrom(messageLite.toByteArray());
            if (secondaryContents.hasItemSectionRenderer()) {
                String sectionIdentifier = secondaryContents
                        .getItemSectionRenderer()
                        .getSectionIdentifier();

                if (sectionIdentifier != null && sectionIdentifier.startsWith(COMMENTS)) {
                    return false;
                }

                isFiltered = true;
                return true;
            }
        } catch (Exception ex) {
            Logger.printException(() -> "Failed to parse ItemSectionRenderer", ex);
        }

        return false;
    }

    /**
     * Identifies tablet shelf renderers, which contain related videos.
     *
     * @param messageLite watch-next secondary content
     * @return whether the content is a shelf renderer
     */
    public static boolean isShelfRenderer(MessageLite messageLite) {
        try {
            SecondaryContents secondaryContents = SecondaryContents.parseFrom(messageLite.toByteArray());
            boolean hasShelfRenderer = secondaryContents.hasShelfRenderer();
            if (hasShelfRenderer) {
                isFiltered = true;
            }

            return hasShelfRenderer;
        } catch (Exception ex) {
            Logger.printException(() -> "Failed to parse ShelfRenderer", ex);
        }

        return false;
    }

    /**
     * @return whether the current response had related content removed
     */
    public static boolean isFiltered() {
        return isFiltered;
    }
}
