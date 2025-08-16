package app.revanced.extension.youtube.patches.components;

import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import java.util.function.Supplier;
import java.util.stream.Stream;

import app.revanced.extension.shared.patches.components.Filter;
import app.revanced.extension.shared.patches.components.StringFilterGroup;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.StringTrieSearch;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.shared.NavigationBar.NavigationButton;
import app.revanced.extension.youtube.shared.RootView;

@SuppressWarnings({"deprecation", "unused"})
public final class CarouselShelfFilter extends Filter {
    private static final String BROWSE_ID_CLIP = "FEclips";
    private static final String BROWSE_ID_COURSES = "FEcourses_destination";
    private static final String BROWSE_ID_HOME = "FEwhat_to_watch";
    private static final String BROWSE_ID_LIBRARY = "FElibrary";
    private static final String BROWSE_ID_LIBRARY_PLAYLIST = "FEplaylist_aggregation";
    private static final String BROWSE_ID_MOVIE = "FEstorefront";
    private static final String BROWSE_ID_NEWS = "FEnews_destination";
    private static final String BROWSE_ID_NOTIFICATION = "FEactivity";
    private static final String BROWSE_ID_NOTIFICATION_INBOX = "FEnotifications_inbox";
    private static final String BROWSE_ID_PLAYLIST = "VLPL";
    private static final String BROWSE_ID_PODCASTS = "FEpodcasts_destination";
    private static final String BROWSE_ID_PREMIUM = "SPunlimited";
    private static final String BROWSE_ID_SUBSCRIPTION = "FEsubscriptions";

    private static final Supplier<Stream<String>> knownBrowseId = () -> Stream.of(
            BROWSE_ID_HOME,
            BROWSE_ID_NOTIFICATION,
            BROWSE_ID_PLAYLIST
    );

    private static final Supplier<Stream<String>> whitelistBrowseId = () -> Stream.of(
            BROWSE_ID_CLIP,
            BROWSE_ID_COURSES,
            BROWSE_ID_LIBRARY,
            BROWSE_ID_LIBRARY_PLAYLIST,
            BROWSE_ID_MOVIE,
            BROWSE_ID_NEWS,
            BROWSE_ID_NOTIFICATION_INBOX,
            BROWSE_ID_PODCASTS,
            BROWSE_ID_PREMIUM
    );

    private final StringTrieSearch exceptions = new StringTrieSearch();

    public CarouselShelfFilter() {
        exceptions.addPattern("library_recent_shelf.eml");

        final StringFilterGroup carouselShelf = new StringFilterGroup(
                null,
                "horizontal_shelf.eml",
                "horizontal_shelf_inline.eml",
                "horizontal_tile_shelf.eml",
                "horizontal_video_shelf.eml"
        );

        addPathCallbacks(carouselShelf);
    }

    private static boolean hideShelves(boolean playerActive, boolean searchBarActive, NavigationButton selectedNavButton, String browseId) {
        final boolean hideHomeAndOthers = Settings.HIDE_CAROUSEL_SHELF_HOME.get();
        final boolean hideSearch = Settings.HIDE_CAROUSEL_SHELF_SEARCH.get();
        final boolean hideSubscriptions = Settings.HIDE_CAROUSEL_SHELF_SUBSCRIPTIONS.get();

        if (!hideHomeAndOthers && !hideSearch && !hideSubscriptions) {
            return false;
        }

        // Must check player type first, as search bar can be active behind the player.
        if (playerActive) {
            return false;
        }

        // Must check second, as search can be from any tab.
        if (searchBarActive) {
            return hideSearch;
        }
        // Unknown tab, treat the same as home.
        if (selectedNavButton == null) {
            return hideHomeAndOthers;
        }
        // Fixes a very rare bug in home.
        if (selectedNavButton == NavigationButton.HOME
                && StringUtils.equalsAny(browseId, BROWSE_ID_LIBRARY, BROWSE_ID_NOTIFICATION_INBOX)) {
            return hideHomeAndOthers;
        }
        boolean isNotWhiteListBrowseId = whitelistBrowseId.get().noneMatch(browseId::equals);
        // Fixes a very rare bug in library.
        if (selectedNavButton == NavigationButton.LIBRARY) {
            return hideHomeAndOthers && isNotWhiteListBrowseId;
        }
        if (BROWSE_ID_SUBSCRIPTION.equals(browseId)) {
            return hideSubscriptions && isNotWhiteListBrowseId;
        }

        return hideHomeAndOthers && (knownBrowseId.get().anyMatch(browseId::equals) || isNotWhiteListBrowseId);
    }

    @Override
    public boolean isFiltered(String path, @Nullable String identifier, String allValue, byte[] protobufBufferArray,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        if (contentIndex != 0 || exceptions.matches(path)) {
            return false;
        }
        final boolean playerActive = RootView.isPlayerActive();
        final boolean searchBarActive = RootView.isSearchBarActive();
        final NavigationButton navigationButton = NavigationButton.getSelectedNavigationButton();
        final String navigation = navigationButton == null ? "null" : navigationButton.name();
        final String browseId = RootView.getBrowseId();
        final boolean hideShelves = hideShelves(playerActive, searchBarActive, navigationButton, browseId);
        Logger.printDebug(() -> "hideShelves: " + hideShelves +
                "\nplayerActive: " + playerActive +
                "\nsearchBarActive: " + searchBarActive +
                "\nbrowseId: " + browseId +
                "\nnavigation: " + navigation);
        if (hideShelves) {
            return super.isFiltered(path, identifier, allValue, protobufBufferArray, matchedGroup, contentType, contentIndex);
        }
        return false;
    }
}
