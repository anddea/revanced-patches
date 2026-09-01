package app.morphe.extension.youtube.patches.video;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;

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
    /**
     * Interface to use obfuscated Shorts quality-menu methods.
     */
    public interface ShortsQualityMenuInterface {
        // Method is added during patching.
        void patch_showShortsQualityMenu();
    }

    private static final boolean ADVANCED_VIDEO_QUALITY_MENU =
            Settings.ADVANCED_VIDEO_QUALITY_MENU.get();
    private static final boolean ADVANCED_VIDEO_QUALITY_MENU_TYPE =
            ADVANCED_VIDEO_QUALITY_MENU && Settings.ADVANCED_VIDEO_QUALITY_MENU_TYPE.get();
    private static WeakReference<ShortsQualityMenuInterface> shortsQualityMenuRef =
            new WeakReference<>(null);

    /**
     * Injection point.
     * <p>
     * Stores the current Shorts quality-menu controller.
     */
    public static void initialize(@NonNull ShortsQualityMenuInterface shortsQualityMenu) {
        shortsQualityMenuRef = new WeakReference<>(shortsQualityMenu);
    }

    /**
     * Injection point.
     * <p>
     * Shorts video quality flyout.
     */
    public static boolean showShortsQualityMenu() {
        if (!ADVANCED_VIDEO_QUALITY_MENU) {
            return false;
        }

        if (ADVANCED_VIDEO_QUALITY_MENU_TYPE) {
            Utils.runOnMainThread(
                    () -> VideoUtils.showCustomVideoQualityFlyoutMenu(Utils.getContext())
            );
            return true;
        }

        ShortsQualityMenuInterface shortsQualityMenu = shortsQualityMenuRef.get();
        if (shortsQualityMenu != null) {
            Utils.runOnMainThread(shortsQualityMenu::patch_showShortsQualityMenu);
            return true;
        }

        return false;
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
