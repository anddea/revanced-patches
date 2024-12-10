package app.revanced.extension.youtube.patches.video;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.Utils;
import app.revanced.extension.youtube.patches.components.VideoQualityMenuFilter;
import app.revanced.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public class RestoreOldVideoQualityMenuPatch {

    public static boolean restoreOldVideoQualityMenu() {
        return Settings.RESTORE_OLD_VIDEO_QUALITY_MENU.get();
    }

    public static void restoreOldVideoQualityMenu(ListView listView) {
        if (!Settings.RESTORE_OLD_VIDEO_QUALITY_MENU.get())
            return;

        listView.setVisibility(View.GONE);

        Utils.runOnMainThreadDelayed(() -> {
                    listView.setSoundEffectsEnabled(false);
                    listView.performItemClick(null, 2, 0);
                },
                1
        );
    }

    public static void onFlyoutMenuCreate(final RecyclerView recyclerView) {
        if (!Settings.RESTORE_OLD_VIDEO_QUALITY_MENU.get())
            return;

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
