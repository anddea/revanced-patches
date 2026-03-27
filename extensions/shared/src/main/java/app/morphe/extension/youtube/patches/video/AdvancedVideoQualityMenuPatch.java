package app.morphe.extension.youtube.patches.video;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.patches.components.VideoQualityMenuFilter;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.utils.VideoUtils;

/**
 * This patch contains the logic to always open the advanced video quality menu.
 */
@SuppressWarnings("unused")
public class AdvancedVideoQualityMenuPatch {
    private static final boolean ADVANCED_VIDEO_QUALITY_MENU =
            Settings.ADVANCED_VIDEO_QUALITY_MENU.get();
    private static final boolean ADVANCED_VIDEO_QUALITY_MENU_TYPE =
            ADVANCED_VIDEO_QUALITY_MENU && Settings.ADVANCED_VIDEO_QUALITY_MENU_TYPE.get();

    /**
     * Injection point.
     * <p>
     * Used in the Shorts video quality flyout, but sometimes also in the regular video quality flyout.
     */
    public static boolean showAdvancedVideoQualityMenu(Context mContext) {
        if (ADVANCED_VIDEO_QUALITY_MENU) {
            Utils.runOnMainThreadDelayed(() -> {
                if (ADVANCED_VIDEO_QUALITY_MENU_TYPE && mContext != null) {
                    VideoUtils.showCustomVideoQualityFlyoutMenu(mContext);
                } else {
                    VideoUtils.showYouTubeLegacyVideoQualityFlyoutMenu();
                }
            }, 100);
        }

        return ADVANCED_VIDEO_QUALITY_MENU;
    }

    /**
     * Injection point.
     * <p>
     * Used in the Shorts video quality flyout, but sometimes also in the regular video quality flyout.
     */
    public static void showAdvancedVideoQualityMenu(ListView listView) {
        if (!ADVANCED_VIDEO_QUALITY_MENU) return;

        listView.setVisibility(View.GONE);
        Utils.runOnMainThreadDelayed(() -> {
                    listView.setSoundEffectsEnabled(false);
                    listView.performItemClick(null, 2, 0);
                },
                1
        );
    }

    /**
     * Injection point.
     */
    public static void onFlyoutMenuCreate(final RecyclerView recyclerView) {
        if (!ADVANCED_VIDEO_QUALITY_MENU) return;

        recyclerView.getViewTreeObserver().addOnDrawListener(() -> {
            try {
                // Check if the current view is the quality menu.
                if (!VideoQualityMenuFilter.isVideoQualityMenuVisible || recyclerView.getChildCount() == 0) {
                    return;
                }

                if (!(Utils.getParentView(recyclerView, 3) instanceof ViewGroup quickQualityViewParent)) {
                    return;
                }

                if (!(recyclerView.getChildAt(0) instanceof ViewGroup advancedQualityParentView)) {
                    return;
                }

                if (advancedQualityParentView.getChildCount() < 4) {
                    return;
                }

                View advancedQualityView = advancedQualityParentView.getChildAt(3);
                if (advancedQualityView == null) {
                    return;
                }

                quickQualityViewParent.setVisibility(View.GONE);

                // Click the "Advanced" quality menu to show the "old" quality menu.
                advancedQualityView.callOnClick();

                VideoQualityMenuFilter.isVideoQualityMenuVisible = false;
            } catch (Exception ex) {
                Logger.printException(() -> "onFlyoutMenuCreate failure", ex);
            }
        });
    }
}
