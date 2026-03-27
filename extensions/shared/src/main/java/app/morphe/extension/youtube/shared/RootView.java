package app.morphe.extension.youtube.shared;

import static app.morphe.extension.youtube.patches.components.LayoutReloadObserverFilter.isActionBarVisible;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.FrameLayout;

import java.lang.ref.WeakReference;

import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;

@SuppressWarnings({"unused", "StaticFieldLeak"})
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
     * Visibility of the ad progress UI component.
     */
    private static volatile int adProgressTextVisibility = -1;

    private static Context mContext;

    /**
     * When a video ad is playing in a regular video player, segments or the Skip button should be hidden.
     *
     * @return Whether the Ad Progress TextView is visible in the regular video player.
     */
    public static boolean isAdProgressTextVisible() {
        return adProgressTextVisibility == View.VISIBLE;
    }

    /**
     * Injection point.
     */
    public static void setAdProgressTextVisibility(int visibility) {
        if (adProgressTextVisibility != visibility) {
            adProgressTextVisibility = visibility;

            Logger.printDebug(() -> {
                String visibilityMessage = switch (visibility) {
                    case View.VISIBLE -> "VISIBLE";
                    case View.GONE -> "GONE";
                    case View.INVISIBLE -> "INVISIBLE";
                    default -> "UNKNOWN";
                };
                return "AdProgressText visibility changed to: " + visibilityMessage;
            });
        }
    }

    /**
     * Injection point.
     */
    public static void setContext(Context context) {
        mContext = context;
    }

    public static Context getContext() {
        return mContext;
    }

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
        return PlayerType.getCurrent().isMaximizedOrFullscreenOrSliding() || isActionBarVisible.get();
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
