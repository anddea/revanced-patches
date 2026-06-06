package app.morphe.extension.youtube.patches.video;

import android.support.v7.widget.RecyclerView;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

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
    private static WeakReference<Object> videoQualityBottomSheetRef = new WeakReference<>(null);

    /**
     * Injection point.
     * <p>
     * Shorts video quality bottom sheet.
     */
    public static void setVideoQualityBottomSheet(Object bottomSheet) {
        videoQualityBottomSheetRef = new WeakReference<>(bottomSheet);
    }

    /**
     * Injection point.
     * <p>
     * Shorts video quality flyout.
     */
    public static void addVideoQualityListMenuListener(ListView listView) {
        if (!ADVANCED_VIDEO_QUALITY_MENU) return;

        listView.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
            @Override
            public void onChildViewAdded(View parent, View child) {
                try {
                    final var indexOfAdvancedQualityMenuItem = 4;
                    if (listView.indexOfChild(child) != indexOfAdvancedQualityMenuItem) return;

                    parent.setVisibility(View.GONE);

                    if (ADVANCED_VIDEO_QUALITY_MENU_TYPE && listView.getContext() != null) {
                        final Context context = listView.getContext();
                        dismissVideoQualityBottomSheet();
                        Utils.runOnMainThreadDelayed(
                                () -> VideoUtils.showCustomVideoQualityFlyoutMenu(context),
                                100
                        );
                    } else {
                        final var qualityItemMenuPosition = 4;
                        listView.setSoundEffectsEnabled(false);
                        listView.performItemClick(null, qualityItemMenuPosition, 0);
                    }
                } catch (Exception ex) {
                    Logger.printException(() -> "showAdvancedVideoQualityMenu failure", ex);
                }
            }

            @Override
            public void onChildViewRemoved(View parent, View child) {
            }
        });
    }

    private static void dismissVideoQualityBottomSheet() {
        final Object bottomSheet = videoQualityBottomSheetRef.get();
        videoQualityBottomSheetRef.clear();
        if (bottomSheet == null) return;

        try {
            final Method dismissMethod = bottomSheet.getClass().getMethod("dismiss");
            dismissMethod.invoke(bottomSheet);
        } catch (Exception ex) {
            Logger.printException(() -> "dismissVideoQualityBottomSheet failure", ex);
        }
    }

    /**
     * Injection point.
     * <p>
     * Used to force the creation of the advanced menu item for the Shorts quality flyout.
     */
    public static boolean forceAdvancedVideoQualityMenuCreation(boolean original) {
        return ADVANCED_VIDEO_QUALITY_MENU || original;
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
