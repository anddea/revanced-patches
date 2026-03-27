package app.morphe.extension.youtube.patches.misc;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.patches.components.ShareSheetMenuFilter;
import app.morphe.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public class ShareSheetPatch {
    private static final boolean CHANGE_SHARE_SHEET = Settings.CHANGE_SHARE_SHEET.get();

    private static void clickSystemShareButton(final RecyclerView bottomSheetRecyclerView,
                                               final RecyclerView appsContainerRecyclerView) {
        if (!(appsContainerRecyclerView.getChildAt(appsContainerRecyclerView.getChildCount() - 1) instanceof ViewGroup parentView)) {
            return;
        }
        if (!(parentView.getChildAt(0) instanceof ViewGroup shareWithOtherAppsView)) {
            return;
        }
        if (!(Utils.getParentView(bottomSheetRecyclerView, 3) instanceof ViewGroup parentView3rd)) {
            return;
        }
        if (!(parentView3rd.getParent() instanceof ViewGroup parentView4th)) {
            return;
        }
        ShareSheetMenuFilter.isShareSheetMenuVisible = false;

        // Dismiss View [R.id.touch_outside] is the 1st ChildView of the 4th ParentView.
        // This only shows in phone layout.
        Utils.clickView(parentView4th.getChildAt(0));

        // In tablet layout there is no Dismiss View, instead we just hide all two parent views.
        parentView3rd.setVisibility(View.GONE);
        parentView4th.setVisibility(View.GONE);

        Utils.clickView(shareWithOtherAppsView);
    }

    /**
     * Injection point.
     */
    public static void onShareSheetMenuCreate(final RecyclerView recyclerView) {
        if (!CHANGE_SHARE_SHEET)
            return;

        recyclerView.getViewTreeObserver().addOnDrawListener(() -> {
            try {
                if (!ShareSheetMenuFilter.isShareSheetMenuVisible) {
                    return;
                }
                if (recyclerView.getChildCount() != 1) {
                    return;
                }
                if (!(recyclerView.getChildAt(0) instanceof ViewGroup parentView5th)) {
                    return;
                }
                if (!(parentView5th.getChildAt(1) instanceof ViewGroup parentView4th)) {
                    return;
                }
                if (parentView4th.getChildAt(0) instanceof ViewGroup parentView3rd &&
                        parentView3rd.getChildAt(0) instanceof RecyclerView appsContainerRecyclerView) {
                    clickSystemShareButton(recyclerView, appsContainerRecyclerView);
                } else if (parentView4th.getChildAt(1) instanceof ViewGroup parentView3rd &&
                        parentView3rd.getChildAt(0) instanceof RecyclerView appsContainerRecyclerView) {
                    clickSystemShareButton(recyclerView, appsContainerRecyclerView);
                }
            } catch (Exception ex) {
                Logger.printException(() -> "onShareSheetMenuCreate failure", ex);
            }
        });
    }

    /**
     * Injection point.
     */
    public static boolean changeShareSheetEnabled() {
        return CHANGE_SHARE_SHEET;
    }

}
