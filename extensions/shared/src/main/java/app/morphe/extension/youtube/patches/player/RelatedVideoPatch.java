package app.morphe.extension.youtube.patches.player;

import androidx.annotation.NonNull;

import java.util.LinkedHashMap;
import java.util.Map;

import app.morphe.extension.shared.utils.Logger;
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

    // video title,channel bar, video action bar, comment
    private static final int MAX_ITEM_COUNT = 4 + OFFSET;

    private static final Map<String, Integer> lastVideoIds = new LinkedHashMap<>() {
        private static final int NUMBER_OF_LAST_VIDEO_IDS_TO_TRACK = 1;

        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > NUMBER_OF_LAST_VIDEO_IDS_TO_TRACK;
        }
    };

    private static String videoId = "";

    public static void newVideoStarted(@NonNull String newlyLoadedChannelId, @NonNull String newlyLoadedChannelName,
                                       @NonNull String newlyLoadedVideoId, @NonNull String newlyLoadedVideoTitle,
                                       final long newlyLoadedVideoLength, boolean newlyLoadedLiveStreamValue) {
        if (!videoId.equals(newlyLoadedVideoId) &&
                PlayerType.getCurrent() != PlayerType.WATCH_WHILE_MINIMIZED) {
            videoId = newlyLoadedVideoId;
            lastVideoIds.clear();
            Logger.printDebug(() -> "newVideoStarted: " + newlyLoadedVideoId);
        }
    }

    public static void onDismiss(int index) {
        if (HIDE_RELATED_VIDEOS && index == 0) {
            videoId = "";
            lastVideoIds.clear();
        }
    }

    public static int overrideItemCounts(int itemCounts) {
        if (!HIDE_RELATED_VIDEOS) {
            return itemCounts;
        }
        if (itemCounts < MAX_ITEM_COUNT) {
            return itemCounts;
        }
        var elements = Thread.currentThread().getStackTrace();
        if (elements.length < 7) {
            return itemCounts;
        }
        var sixthElement = elements[6];
        if (sixthElement == null) {
            return itemCounts;
        }
        if (!sixthElement.toString().startsWith(SCROLL_TO_TOP_LINEAR_LAYOUT_MANAGER_CLASS)) {
            return itemCounts;
        }
        if (videoId.isEmpty()) {
            return itemCounts;
        }
        Integer count = lastVideoIds.get(videoId);
        if (count != null && itemCounts == count &&
                PlayerType.getCurrent().isMaximizedOrFullscreenOrSliding()) {
            return MAX_ITEM_COUNT;
        }
        if (!RootView.isPlayerActive()) {
            return itemCounts;
        }
        if (BottomSheetState.getCurrent().isOpen()) {
            return itemCounts;
        }
        if (EngagementPanel.isOpen()) {
            return itemCounts;
        }
        if (count == null) {
            lastVideoIds.put(videoId, itemCounts);
            return MAX_ITEM_COUNT;
        } else if (PlayerType.getCurrent().isMaximizedOrFullscreenOrSliding() &&
                Math.abs(itemCounts - count) < 5) {
            return MAX_ITEM_COUNT;
        } else {
            return itemCounts;
        }
    }
}
