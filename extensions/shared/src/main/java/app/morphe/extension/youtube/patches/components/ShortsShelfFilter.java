package app.morphe.extension.youtube.patches.components;

import org.apache.commons.lang3.StringUtils;

import app.morphe.extension.shared.patches.components.ByteArrayFilterGroup;
import app.morphe.extension.shared.patches.components.Filter;
import app.morphe.extension.shared.patches.components.StringFilterGroup;
import app.morphe.extension.shared.settings.BooleanSetting;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.StringTrieSearch;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.shared.EngagementPanel;
import app.morphe.extension.youtube.shared.NavigationBar.NavigationButton;
import app.morphe.extension.youtube.shared.RootView;

@SuppressWarnings({"unused", "deprecation"})
public final class ShortsShelfFilter extends Filter {
    private static final String BROWSE_ID_HISTORY = "FEhistory";
    private static final String BROWSE_ID_LIBRARY = "FElibrary";
    private static final String BROWSE_ID_NOTIFICATION_INBOX = "FEnotifications_inbox";
    private static final String BROWSE_ID_SUBSCRIPTIONS = "FEsubscriptions";
    private static final String CONVERSATION_CONTEXT_FEED_IDENTIFIER =
            "horizontalCollectionSwipeProtector=null";
    private static final String SHELF_HEADER_PATH = "shelf_header.";
    private final StringFilterGroup channelProfile;
    private final StringFilterGroup compactFeedVideoPath;
    private final ByteArrayFilterGroup compactFeedVideoBuffer;
    private final StringFilterGroup shelfHeaderIdentifier;
    private final StringFilterGroup shelfHeaderPath;
    private static final StringTrieSearch feedGroup = new StringTrieSearch();
    private static final BooleanSetting hideShortsShelf = Settings.HIDE_SHORTS_SHELF;
    private static final BooleanSetting hideChannel = Settings.HIDE_SHORTS_SHELF_CHANNEL;
    private static final ByteArrayFilterGroup channelProfileShelfHeader =
            new ByteArrayFilterGroup(
                    hideChannel,
                    "Shorts"
            );

    public ShortsShelfFilter() {
        feedGroup.addPattern(CONVERSATION_CONTEXT_FEED_IDENTIFIER);

        channelProfile = new StringFilterGroup(
                hideChannel,
                "shorts_pivot_item"
        );

        final StringFilterGroup shortsIdentifiers = new StringFilterGroup(
                hideShortsShelf,
                "shorts_shelf",
                "inline_shorts",
                "shorts_grid",
                "shorts_video_cell"
        );

        shelfHeaderIdentifier = new StringFilterGroup(
                hideShortsShelf,
                SHELF_HEADER_PATH
        );

        addIdentifierCallbacks(channelProfile, shortsIdentifiers, shelfHeaderIdentifier);

        compactFeedVideoPath = new StringFilterGroup(
                hideShortsShelf,
                // Shorts that appear in the feed/search when the device is using tablet layout.
                "compact_video.",
                // 'video_lockup_with_attachment.' is used instead of 'compact_video.' for some users. (A/B tests)
                "video_lockup_with_attachment.",
                // Search results that appear in a horizontal shelf.
                "video_card."
        );

        // Filter out items that use the 'frame0' thumbnail.
        // This is a valid thumbnail for both regular videos and Shorts,
        // but it appears these thumbnails are used only for Shorts.
        compactFeedVideoBuffer = new ByteArrayFilterGroup(
                hideShortsShelf,
                "/frame0.jpg"
        );

        // Feed Shorts shelf header.
        // Use a different filter group for this pattern, as it requires an additional check after matching.
        shelfHeaderPath = new StringFilterGroup(
                hideShortsShelf,
                SHELF_HEADER_PATH
        );

        addPathCallbacks(compactFeedVideoPath, shelfHeaderPath);
    }

    @Override
    public boolean isFiltered(String path, String identifier, String allValue, byte[] buffer,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        final boolean playerActive = RootView.isPlayerActive();
        final boolean descriptionActive = EngagementPanel.isDescription();
        final boolean searchBarActive = RootView.isSearchBarActive();
        final NavigationButton navigationButton = NavigationButton.getSelectedNavigationButton();
        final String navigation = navigationButton == null ? "null" : navigationButton.name();
        final String browseId = RootView.getBrowseId();
        final boolean hideShelves = shouldHideShortsFeedItems(playerActive, descriptionActive, searchBarActive, navigationButton, browseId);
        Logger.printDebug(() -> "hideShelves: " + hideShelves +
                "\nplayerActive: " + playerActive +
                "\ndescriptionActive: " + descriptionActive +
                "\nsearchBarActive: " + searchBarActive +
                "\nbrowseId: " + browseId +
                "\nnavigation: " + navigation
        );
        if (contentType == FilterContentType.PATH) {
            if (matchedGroup == compactFeedVideoPath) {
                return hideShelves && compactFeedVideoBuffer.check(buffer).isFiltered();
            } else if (matchedGroup == shelfHeaderPath) {
                // Because the header is used in watch history and possibly other places, check for the index,
                // which is 0 when the shelf header is used for Shorts.
                if (contentIndex != 0) {
                    return false;
                }
                if (!channelProfileShelfHeader.check(buffer).isFiltered()) {
                    return false;
                }
                return !feedGroup.matches(allValue);
            }
        } else if (contentType == FilterContentType.IDENTIFIER) {
            // Feed/search identifier components.
            if (matchedGroup == shelfHeaderIdentifier) {
                // Check ConversationContext to not hide shelf header in channel profile
                // This value does not exist in the shelf header in the channel profile
                if (!feedGroup.matches(allValue)) {
                    return false;
                }
            } else if (matchedGroup == channelProfile) {
                return true;
            }
            return hideShelves;
        }

        return true;
    }

    private static boolean shouldHideShortsFeedItems(boolean playerActive, boolean descriptionActive, boolean searchBarActive, NavigationButton selectedNavButton, String browseId) {
        final boolean hideHomeAndRelatedVideos = Settings.HIDE_SHORTS_SHELF_HOME_RELATED_VIDEOS.get();
        final boolean hideSearch = Settings.HIDE_SHORTS_SHELF_SEARCH.get();
        final boolean hideSubscriptions = Settings.HIDE_SHORTS_SHELF_SUBSCRIPTIONS.get();
        final boolean hideVideoDescription = Settings.HIDE_SHORTS_SHELF_VIDEO_DESCRIPTION.get();
        final boolean hideHistory = Settings.HIDE_SHORTS_SHELF_HISTORY.get();

        if (hideHomeAndRelatedVideos && hideSearch && hideSubscriptions && hideVideoDescription && hideHistory) {
            // Shorts suggestions can load in the background if a video is opened and
            // then immediately minimized before any suggestions are loaded.
            // In this state the player type will show minimized, which makes it not possible to
            // distinguish between Shorts suggestions loading in the player and between
            // scrolling thru search/home/subscription tabs while a player is minimized.
            //
            // To avoid this situation for users that never want to show Shorts (all hide Shorts options are enabled)
            // then hide all Shorts everywhere including the Library history and Library playlists.
            return true;
        }

        // Must check player type first, as search bar can be active behind the player.
        if (playerActive) {
            return descriptionActive
                    ? hideVideoDescription      // Player video description panel opened.
                    : hideHomeAndRelatedVideos; // For now, consider the under video results the same as the home feed.
        }

        // Must check second, as search can be from any tab.
        if (searchBarActive) {
            return hideSearch;
        }

        // Avoid checking navigation button status if all other Shorts should show.
        if (!hideHomeAndRelatedVideos && !hideSubscriptions && !hideHistory) {
            return false;
        }

        // Unknown tab, treat the same as home.
        if (selectedNavButton == null) {
            return hideHomeAndRelatedVideos;
        }

        // Fixes a very rare bug in home.
        if (selectedNavButton == NavigationButton.HOME
                && StringUtils.equalsAny(browseId, BROWSE_ID_LIBRARY, BROWSE_ID_NOTIFICATION_INBOX)) {
            return hideHomeAndRelatedVideos;
        }

        switch (browseId) {
            case BROWSE_ID_HISTORY, BROWSE_ID_LIBRARY, BROWSE_ID_NOTIFICATION_INBOX -> {
                return hideHistory;
            }
            case BROWSE_ID_SUBSCRIPTIONS -> {
                return hideSubscriptions;
            }
            default -> {
                return hideHomeAndRelatedVideos;
            }
        }
    }
}
