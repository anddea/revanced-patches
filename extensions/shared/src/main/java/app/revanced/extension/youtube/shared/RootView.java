package app.revanced.extension.youtube.shared;

import static app.revanced.extension.youtube.patches.components.RelatedVideoFilter.isActionBarVisible;

import android.graphics.drawable.Drawable;
import android.widget.FrameLayout;

import java.lang.ref.WeakReference;

import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.Utils;

@SuppressWarnings("unused")
public final class RootView {

    /**
     * Interface to call obfuscated methods in AppCompat Toolbar class.
     */
    public interface AppCompatToolbarPatchInterface {
        Drawable patch_getToolbarIcon();
    }

    private static volatile WeakReference<AppCompatToolbarPatchInterface> toolbarResultsRef
            = new WeakReference<>(null);

    /**
     * Injection point.
     */
    public static void setToolbar(FrameLayout layout) {
        AppCompatToolbarPatchInterface toolbar = Utils.getChildView(layout, false, (view) ->
                view instanceof AppCompatToolbarPatchInterface
        );

        if (toolbar == null) {
            Logger.printException(() -> "Could not find toolbar");
            return;
        }

        toolbarResultsRef = new WeakReference<>(toolbar);
    }

    public static boolean isBackButtonVisible() {
        AppCompatToolbarPatchInterface toolbar = toolbarResultsRef.get();
        return toolbar != null && toolbar.patch_getToolbarIcon() != null;
    }

    /**
     * @return If the search bar is on screen.  This includes if the player
     * is on screen and the search results are behind the player (and not visible).
     * Detecting the search is covered by the player can be done by checking {@link RootView#isPlayerActive()}.
     */
    public static boolean isSearchBarActive() {
        String searchQuery = getSearchQuery();
        return !searchQuery.isEmpty();
    }

    public static boolean isPlayerActive() {
        return PlayerType.getCurrent().isMaximizedOrFullscreen() || isActionBarVisible.get();
    }

    public static boolean isShortsActive() {
        return ShortsPlayerState.getCurrent().isOpen();
    }

    /**
     * Get current BrowseId.
     * Rest of the implementation added by patch.
     */
    public static String getBrowseId() {
        return "";
    }

    /**
     * Get current SearchQuery.
     * Rest of the implementation added by patch.
     */
    public static String getSearchQuery() {
        return "";
    }
}
