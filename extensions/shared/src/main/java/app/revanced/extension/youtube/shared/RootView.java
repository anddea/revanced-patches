package app.revanced.extension.youtube.shared;

import static app.revanced.extension.youtube.patches.components.RelatedVideoFilter.isActionBarVisible;

@SuppressWarnings("unused")
public final class RootView {

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
