package app.revanced.extension.youtube.patches.feed;

import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.shared.BottomSheetState;
import app.revanced.extension.youtube.shared.EngagementPanel;
import app.revanced.extension.youtube.shared.RootView;

@SuppressWarnings("unused")
public final class RelatedVideoPatch {
    private static final boolean HIDE_RELATED_VIDEOS = Settings.HIDE_RELATED_VIDEOS.get();
    private static final int OFFSET = Settings.RELATED_VIDEOS_OFFSET.get();

    // video title,channel bar, video action bar, comment
    private static final int MAX_ITEM_COUNT = 4 + OFFSET;

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
        return MAX_ITEM_COUNT;
    }

}
