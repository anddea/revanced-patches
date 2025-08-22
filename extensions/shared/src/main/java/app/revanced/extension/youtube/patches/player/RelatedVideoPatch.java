package app.revanced.extension.youtube.patches.player;

import java.util.LinkedHashMap;
import java.util.Map;

import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.shared.BottomSheetState;
import app.revanced.extension.youtube.shared.EngagementPanel;
import app.revanced.extension.youtube.shared.RootView;
import app.revanced.extension.youtube.shared.VideoInformation;

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

    public static void onDismiss(int index) {
        if (HIDE_RELATED_VIDEOS && index == 0) {
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
        if (!RootView.isPlayerActive()) {
            return itemCounts;
        }
        if (BottomSheetState.getCurrent().isOpen()) {
            return itemCounts;
        }
        if (EngagementPanel.isOpen()) {
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

        String videoId = VideoInformation.getVideoId();
        Integer count = lastVideoIds.get(videoId);
        if (count == null) {
            lastVideoIds.put(videoId, itemCounts);
        } else if (count != itemCounts) {
            return itemCounts;
        }

        return MAX_ITEM_COUNT;
    }

}
