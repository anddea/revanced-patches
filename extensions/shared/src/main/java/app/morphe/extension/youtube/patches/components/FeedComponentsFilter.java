/*
 * Portions of this file are ported from Morphe:
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.extension.youtube.patches.components;


import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import app.morphe.extension.shared.patches.components.ByteArrayFilterGroup;
import app.morphe.extension.shared.patches.components.ByteArrayFilterGroupList;
import app.morphe.extension.shared.patches.components.Filter;
import app.morphe.extension.shared.patches.components.BufferAsciiStrings;
import app.morphe.extension.shared.patches.components.FilterGroup.FilterGroupResult;
import app.morphe.extension.youtube.patches.utils.ProtoNode;
import app.morphe.extension.shared.patches.components.StringFilterGroup;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.StringTrieSearch;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.shared.EngagementPanel;
import app.morphe.extension.youtube.shared.NavigationBar;
import app.morphe.extension.youtube.shared.NavigationBar.NavigationButton;
import app.morphe.extension.youtube.shared.RootView;
import app.morphe.extension.youtube.utils.ExtendedUtils;

@SuppressWarnings({"deprecation", "unused", "FieldCanBeLocal"})
public final class FeedComponentsFilter extends Filter {
    private final String BROWSE_ID_CLIP = "FEclips";
    private final String BROWSE_ID_COURSES = "FEcourses_destination";
    private final String BROWSE_ID_HISTORY = "FEhistory";
    private final String BROWSE_ID_HOME = "FEwhat_to_watch";
    private final String BROWSE_ID_LIBRARY = "FElibrary";
    private final String BROWSE_ID_LIBRARY_PLAYLIST = "FEplaylist_aggregation";
    private final String BROWSE_ID_MOVIE = "FEstorefront";
    private final String BROWSE_ID_NEWS = "FEnews_destination";
    private final String BROWSE_ID_NOTIFICATION = "FEactivity";
    private final String BROWSE_ID_NOTIFICATION_INBOX = "FEnotifications_inbox";
    private final String BROWSE_ID_PLAYLIST = "VLPL";
    private final String BROWSE_ID_PODCASTS = "FEpodcasts_destination";
    private final String BROWSE_ID_PREMIUM = "SPunlimited";
    private final String BROWSE_ID_SUBSCRIPTION = "FEsubscriptions";

    private final String INLINE_EXPANSION_PATH = "inline_expansion";
    private final String FEED_VIDEO_PATH = "video_lockup_with_attachment";

    private final StringFilterGroup channelProfileSubscribeButton;
    private final ByteArrayFilterGroup channelProfileSubscribeButtonBuffer;
    private final ByteArrayFilterGroup buttonComponentBuffer;
    private final StringFilterGroup carouselShelves;
    private final StringFilterGroup chipBar;
    private final StringFilterGroup chipsShelf;
    private final StringFilterGroup compactChannel;
    private final StringFilterGroup communityPosts;
    private final StringFilterGroup expandableCard;
    private final ByteArrayFilterGroup productCardBuffer;
    private final ByteArrayFilterGroup summaryCardBuffer;
    private final ByteArrayFilterGroup playablesBuffer;
    private final ByteArrayFilterGroup ticketShelfBuffer;
    private final ByteArrayFilterGroup movieShelfBuffer;
    private final StringFilterGroup inviteToMessageCard;
    private final ByteArrayFilterGroup inviteToMessageCardBuffer;
    private final StringFilterGroup liveStream;
    private final ByteArrayFilterGroup liveStreamBuffer;
    private final StringFilterGroup videoLabels;
    private final ByteArrayFilterGroupList videoLabelsGroupList = new ByteArrayFilterGroupList();
    private final StringFilterGroup videoRecommendationLabels;

    private final Supplier<Stream<String>> knownBrowseId = () -> Stream.of(
            BROWSE_ID_HOME,
            BROWSE_ID_NOTIFICATION,
            BROWSE_ID_PLAYLIST
    );

    private final Supplier<Stream<String>> whitelistBrowseId = () -> Stream.of(
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

    private final StringTrieSearch carouselShelfExceptions = new StringTrieSearch();

    private static final ByteArrayFilterGroup mixPlaylistsBufferExceptions = new ByteArrayFilterGroup(
            null,
            "cell_description_body",
            "channel_profile"
    );
    private static final ByteArrayFilterGroup mixPlaylistUrlBuffer = new ByteArrayFilterGroup(
            null,
            "?list=RD",
            "&list=RD"
    );

    public enum ExpandableCardStyle {
        SHOW_ALL,
        HIDE_PRODUCT_ONLY,
        HIDE_SUMMARY_ONLY,
        HIDE_PRODUCT_AND_SUMMARY,
        HIDE_ALL
    }

    public FeedComponentsFilter() {
        carouselShelfExceptions.addPattern("library_recent_shelf.");

        // Identifiers.

        chipsShelf = new StringFilterGroup(
                Settings.HIDE_CHIPS_SHELF,
                "chips_shelf"
        );

        communityPosts = new StringFilterGroup(
                null,
                "images_post_responsive",
                "images_post_root",
                "images_post_slim",
                "options_post_responsive_root",
                "options_post_root",
                "poll_post_responsive_root",
                "poll_post_root",
                "post_base_wrapper",
                "post_responsive_root",
                "post_shelf_slim",
                "shared_post_responsive_root",
                "shared_post_root",
                "text_post_responsive_root",
                "text_post_root",
                "videos_post_responsive_root",
                "videos_post_root"
        );

        final StringFilterGroup expandableShelf = new StringFilterGroup(
                Settings.HIDE_EXPANDABLE_SHELF,
                "expandable_section"
        );

        final StringFilterGroup feedSearchBar = new StringFilterGroup(
                Settings.HIDE_FEED_SEARCH_BAR,
                "search_bar_entry_point"
        );

        final StringFilterGroup movieShelfIdentifier = new StringFilterGroup(
                Settings.HIDE_MOVIE_SHELF,
                "tvfilm_attachment"
        );

        final StringFilterGroup ticketShelfIdentifier = new StringFilterGroup(
                Settings.HIDE_TICKET_SHELF,
                "ticket_"
        );

        // The 'Invite others to message' card of the Messages section shown at the top of
        // the Notifications tab, wrapped in a linear layout and identified by a unique,
        // language independent buffer string.
        //
        // The 'Messages' shelf header above the card is deliberately not hidden: every
        // section header of the Notifications tab ('Messages', 'Notifications', 'Today',
        // 'This week', 'Older') uses the exact same identifier and an otherwise byte
        // identical buffer, and the title is localized by the server without an app string
        // resource, so there is no language independent way to match it.
        inviteToMessageCard = new StringFilterGroup(
                Settings.HIDE_INVITE_TO_MESSAGE_CARD,
                "linear_layout.e"
        );

        inviteToMessageCardBuffer = new ByteArrayFilterGroup(
                null,
                "connections_inbox_zero_state"
        );

        // Feed and search result items.
        liveStream = new StringFilterGroup(
                Settings.HIDE_LIVE_STREAMS,
                "video_lockup_with_attachment.e"
        );

        // Icon of the 'LIVE' thumbnail badge. Only videos that are live now have it,
        // past streams ('Streamed 2 months ago') do not.
        liveStreamBuffer = new ByteArrayFilterGroup(
                null,
                "yt_outline_live_black",
                "yt_outline_experimental_live_black"
        );

        addIdentifierCallbacks(
                chipsShelf,
                communityPosts,
                expandableShelf,
                feedSearchBar,
                movieShelfIdentifier,
                ticketShelfIdentifier,
                inviteToMessageCard,
                liveStream
        );

        // Paths.

        final StringFilterGroup albumCard = new StringFilterGroup(
                Settings.HIDE_ALBUM_CARDS,
                "browsy_bar",
                "official_card"
        );

        channelProfileSubscribeButton = new StringFilterGroup(
                Settings.HIDE_SUBSCRIBE_BUTTON_IN_CHANNEL_PAGE,
                "channel_action_buttons_phone.e"
        );
        channelProfileSubscribeButtonBuffer = new ByteArrayFilterGroup(
                null,
                "subscribe_button.e"
        );
        buttonComponentBuffer = new ByteArrayFilterGroup(
                null,
                "button.e"
        );

        compactChannel = new StringFilterGroup(
                Settings.HIDE_CHANNEL_BUTTONS,
                "compact_channel.e"
        );

        final StringFilterGroup membersShelf = new StringFilterGroup(
                Settings.HIDE_MEMBERS_SHELF,
                "member_recognition_shelf"
        );

        final StringFilterGroup linksPreview = new StringFilterGroup(
                Settings.HIDE_LINKS_PREVIEW,
                "channel_header_links",
                "attribution.e" // new layout
        );

        expandableCard = new StringFilterGroup(
                null,
                INLINE_EXPANSION_PATH,
                "inline_expander",
                "expandable_metadata."
        );

        productCardBuffer = new ByteArrayFilterGroup(
                null,
                "gstatic.com/shopping"
        );

        summaryCardBuffer = new ByteArrayFilterGroup(
                null,
                "PAfeedback_genai"
        );

        final StringFilterGroup surveys = new StringFilterGroup(
                Settings.HIDE_SURVEYS,
                "in_feed_survey",
                "slimline_survey",
                "feed_nudge",
                "in_short_survey"
        );

        // It appears YouTube no longer uses this keyword.
        // Just in case, I won't remove this filter until 2025.
        final StringFilterGroup forYouShelf = new StringFilterGroup(
                Settings.HIDE_CAROUSEL_SHELF_HOME,
                "mixed_content_shelf"
        );

        final StringFilterGroup imageShelf = new StringFilterGroup(
                Settings.HIDE_IMAGE_SHELF,
                "image_shelf"
        );

        final StringFilterGroup latestPosts = new StringFilterGroup(
                Settings.HIDE_LATEST_POSTS,
                "post_shelf"
        );

        final StringFilterGroup movieShelf = new StringFilterGroup(
                Settings.HIDE_MOVIE_SHELF,
                "compact_movie",
                "horizontal_movie_shelf",
                "movie_and_show_upsell_card",
                "compact_tvfilm_item",
                "offer_module"
        );

        final StringFilterGroup notifyMe = new StringFilterGroup(
                Settings.HIDE_NOTIFY_ME_BUTTON,
                "set_reminder_button"
        );

        final StringFilterGroup playables = new StringFilterGroup(
                Settings.HIDE_PLAYABLES,
                "horizontal_gaming_shelf",
                "mini_game_card."
        );

        final StringFilterGroup subscribedChannelsBar = new StringFilterGroup(
                Settings.HIDE_SUBSCRIBED_CHANNELS_BAR,
                "subscriptions_channel_bar"
        );

        final StringFilterGroup subscriptionsCategoryBar = new StringFilterGroup(
                Settings.HIDE_CATEGORY_BAR_IN_FEED,
                "subscriptions_chip_bar"
        );

        final StringFilterGroup subscriptionsSectionHeader = new StringFilterGroup(
                Settings.HIDE_SECTION_HEADER_IN_FEED,
                "subscriptions_section_header"
        );

        videoRecommendationLabels = new StringFilterGroup(
                Settings.HIDE_VIDEO_RECOMMENDATION_LABELS,
                "endorsement_header_footer."
        );

        videoLabels = new StringFilterGroup(
                null,
                "|badge.e"
        );
        videoLabelsGroupList.addAll(
                new ByteArrayFilterGroup(
                        Settings.HIDE_AUTO_DUBBED_LABEL,
                        "yt_outline_person_radar",
                        "yt_outline_experimental_person_waves"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_HYPED_LABEL,
                        "yt_fill_star_shooting",
                        "yt_fill_experimental_hype"
                )
        );

        carouselShelves = new StringFilterGroup(
                null,
                "horizontal_video_shelf.",
                "horizontal_shelf.",
                "horizontal_shelf_inline.",
                "horizontal_tile_shelf."
        );

        chipBar = new StringFilterGroup(
                null,
                "chip_bar"
        );

        final StringFilterGroup ticketShelfPath = new StringFilterGroup(
                Settings.HIDE_TICKET_SHELF,
                "ticket_horizontal_shelf",
                "ticket_shelf"
        );

        playablesBuffer = new ByteArrayFilterGroup(
                Settings.HIDE_PLAYABLES,
                "mini_game"
        );

        ticketShelfBuffer = new ByteArrayFilterGroup(
                Settings.HIDE_TICKET_SHELF,
                "ticket_item"
        );

        movieShelfBuffer = new ByteArrayFilterGroup(
                Settings.HIDE_MOVIE_SHELF,
                "movie_card.e"
        );

        addPathCallbacks(
                albumCard,
                carouselShelves,
                channelProfileSubscribeButton,
                chipBar,
                compactChannel,
                expandableCard,
                forYouShelf,
                imageShelf,
                latestPosts,
                linksPreview,
                membersShelf,
                movieShelf,
                notifyMe,
                playables,
                subscribedChannelsBar,
                subscriptionsCategoryBar,
                subscriptionsSectionHeader,
                surveys,
                ticketShelfPath,
                videoLabels,
                videoRecommendationLabels
        );
    }

    /**
     * Injection point.
     * <p>
     * Called from a different place then the other filters.
     */
    public static boolean filterMixPlaylists(@Nullable final byte[] bytes) {
        try {
            if (!Settings.HIDE_MIX_PLAYLISTS.get()) {
                return false;
            }

            if (bytes == null) {
                Logger.printDebug(() -> "buffer is null");
                return false;
            }

            if (!mixPlaylistsBufferExceptions.check(bytes).isFiltered()
                    && mixPlaylistUrlBuffer.check(bytes).isFiltered()) {
                Logger.printDebug(() -> "Filtered mix playlist");
                return true;
            }
        } catch (Exception ex) {
            Logger.printException(() -> "filterMixPlaylists failure", ex);
        }

        return false;
    }

    private boolean hideCategoryBar(int contentIndex) {
        if (contentIndex != 0) {
            return false;
        }

        final boolean hideHistory = Settings.HIDE_CATEGORY_BAR_IN_HISTORY.get();
        final boolean hidePlaylist = Settings.HIDE_CATEGORY_BAR_IN_PLAYLIST.get();

        if (hideHistory && hidePlaylist) {
            return true;
        }

        if (!hideHistory && !hidePlaylist) {
            return false;
        }

        // Must check player type first, as search bar can be active behind the player.
        if (RootView.isPlayerActive()) {
            return false;
        }

        // Must check second, as search can be from any tab.
        if (RootView.isSearchBarActive()) {
            return false;
        }

        String browseId = RootView.getBrowseId();
        return (browseId.equals(BROWSE_ID_HISTORY) && hideHistory) || hidePlaylist;
    }

    private boolean hideShelves() {
        final boolean hideHomeAndOthers = Settings.HIDE_CAROUSEL_SHELF_HOME.get();
        final boolean hideSearch = Settings.HIDE_CAROUSEL_SHELF_SEARCH.get();
        final boolean hideSubscriptions = Settings.HIDE_CAROUSEL_SHELF_SUBSCRIPTIONS.get();

        if (!hideHomeAndOthers && !hideSearch && !hideSubscriptions) {
            return false;
        }

        // Must check player type first, as search bar can be active behind the player.
        if (RootView.isPlayerActive()) {
            return hideHomeAndOthers
                    && !EngagementPanel.isDescription()
                    && NavigationButton.getSelectedNavigationButton() != NavigationButton.LIBRARY;
        }

        // Must check second, as search can be from any tab.
        if (RootView.isSearchBarActive()) {
            return hideSearch;
        }

        NavigationButton selectedNavButton = NavigationButton.getSelectedNavigationButton();
        // Unknown tab, treat the same as home.
        if (selectedNavButton == null) {
            return hideHomeAndOthers;
        }

        String browseId = RootView.getBrowseId();
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

    /**
     * The subscribe button can't be removed from the data of the page header like the other buttons,
     * as the header keeps its space. Hide the button and the container that wraps it instead.
     *
     * @param path         Path of an element of the channel page header action buttons.
     * @param buffer       Buffer of the element.
     * @param asciiStrings Texts of the buffer.
     */
    private boolean isChannelProfileSubscribeButtonHidden(CharSequence path, byte[] buffer,
                                                          BufferAsciiStrings asciiStrings) {
        if (!buttonComponentBuffer.check(buffer).isFiltered()) {
            // The spacers between the buttons are the elements without texts outside the buttons.
            return hideChannelProfileHeaderSpacers
                    && !StringUtils.contains(path, "button.e")
                    && asciiStrings.getStrings().isEmpty();
        }

        return isSingleButton(buffer) && channelProfileSubscribeButtonBuffer.check(buffer).isFiltered();
    }

    /**
     * Hiding only the content of a button leaves the button and the container that wraps it,
     * and their flex style keeps the space of the button and shrinks the other buttons.
     * The buffer of a container holds the buffers of all its children,
     * so the elements to hide are the ones that hold a single button.
     */
    private boolean isSingleButton(byte[] buffer) {
        FilterGroupResult button = buttonComponentBuffer.check(buffer);
        return button.isFiltered() && !buttonComponentBuffer.check(buffer,
                button.getMatchedIndex() + button.getMatchedLength()).isFiltered();
    }

    /**
     * Only the root lockup holds the whole item buffer. Its nested components share
     * a common icon list that also contains the LIVE badge, so checking them would
     * cause false positives.
     *
     * @return If the path is the root of a feed item: {@code identifier|hash|CellType|}.
     */
    private static boolean isLockupRoot(CharSequence path) {
        return StringUtils.endsWith(path, "|CellType|");
    }

    @Override
    public boolean isFiltered(String path, String accessibility, String allValue, byte[] buffer,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        if (matchedGroup == channelProfileSubscribeButton) {
            return isChannelProfileSubscribeButtonHidden(path, buffer, new BufferAsciiStrings(buffer));
        }

        if (matchedGroup == chipBar) {
            return hideCategoryBar(contentIndex);
        }

        if (matchedGroup == chipsShelf) {
            return NavigationButton.getSelectedNavigationButton() != NavigationButton.LIBRARY;
        }

        if (matchedGroup == compactChannel) {
            return StringUtils.contains(path, "button.e");
        }

        if (matchedGroup == communityPosts) {
            // Channel Pages (Deep navigation logic)
            // When back button is visible, we are likely on a channel page.
            // Exclude Player and Search to ensure we don't accidentally hide Related/Search items with this setting.
            if (RootView.isBackButtonVisible()
                    && !RootView.isSearchBarActive()
                    && !RootView.isPlayerActive()) {
                return Settings.HIDE_COMMUNITY_POSTS_CHANNEL.get();
            }

            // Subscriptions Feed
            NavigationButton navButton = NavigationButton.getSelectedNavigationButton();
            String browseId = RootView.getBrowseId();

            boolean isSubscriptions = (navButton == NavigationButton.SUBSCRIPTIONS)
                    || BROWSE_ID_SUBSCRIPTION.equals(browseId);

            if (isSubscriptions) {
                return Settings.HIDE_COMMUNITY_POSTS_SUBSCRIPTIONS.get();
            }

            // If we are not in Channel or Subscriptions, we assume Home or Related Videos.
            return Settings.HIDE_COMMUNITY_POSTS_HOME_RELATED_VIDEOS.get();
        }

        if (matchedGroup == expandableCard) {
            if (!path.startsWith(FEED_VIDEO_PATH)) {
                return false;
            }

            ExpandableCardStyle style = Settings.HIDE_EXPANDABLE_CARD.get();
            return switch (style) {
                case HIDE_ALL -> true;
                case HIDE_PRODUCT_ONLY -> productCardBuffer.check(buffer).isFiltered();
                case HIDE_SUMMARY_ONLY -> summaryCardBuffer.check(buffer).isFiltered();
                case HIDE_PRODUCT_AND_SUMMARY ->
                        productCardBuffer.check(buffer).isFiltered() || summaryCardBuffer.check(buffer).isFiltered();
                default -> false;
            };
        }

        if (matchedGroup == carouselShelves) {
            if (contentIndex == 0) {
                return playablesBuffer.check(buffer).isFiltered()
                        || ticketShelfBuffer.check(buffer).isFiltered()
                        || movieShelfBuffer.check(buffer).isFiltered()
                        || (!carouselShelfExceptions.matches(path) && hideShelves());
            }
            return false;
        }

        if (matchedGroup == inviteToMessageCard) {
            // The identifier is generic and used all over the app.
            if (contentIndex != 0) {
                return false;
            }

            if (!inviteToMessageCardBuffer.check(buffer).isFiltered()) {
                return false;
            }

            // Check the navigation button last and only after all buffer checks pass.
            return NavigationButton.getSelectedNavigationButton() == NavigationButton.NOTIFICATIONS;
        }

        if (matchedGroup == liveStream) {
            // Only check the whole lockup. The buffers of its nested components include
            // a shared list of icon names that also has the 'LIVE' badge icon.
            return isLockupRoot(path) && liveStreamBuffer.check(buffer).isFiltered();
        }

        if (matchedGroup == videoLabels) {
            return ExtendedUtils.IS_20_31_OR_GREATER && videoLabelsGroupList.check(buffer).isFiltered();
        }

        if (matchedGroup == videoRecommendationLabels) {
            return NavigationBar.isSearchBarActive();
        }

        return true;
    }
    /**
     * Field numbers of the view models of the buttons, in the proto of the elements.
     */
    private static final int BUTTON_VIEW_MODEL_FIELD = 461054335;
    private static final int SUBSCRIBE_BUTTON_VIEW_MODEL_FIELD = 518912898;

    private static final ByteArrayFilterGroup pageHeaderBuffer = new ByteArrayFilterGroup(
            null,
            "page_header.e"
    );

    /**
     * Ids of the page header buttons: the accessibility ids (such as 'id.sponsor_button'),
     * or the browse id of the Premium page.
     */
    private static final StringFilterGroup[] pageHeaderButtonIds = {
            new StringFilterGroup(
                    Settings.HIDE_GET_PREMIUM_BUTTON,
                    "SPunlimited"
            ),
            new StringFilterGroup(
                    Settings.HIDE_COMMUNITY_BUTTON,
                    "header_community_button"
            ),
            new StringFilterGroup(
                    Settings.HIDE_JOIN_BUTTON_IN_CHANNEL_PAGE,
                    "sponsor_button"
            ),
            new StringFilterGroup(
                    Settings.HIDE_STORE_BUTTON,
                    "header_store_button"
            )
    };
    private static final ByteArrayFilterGroupList pageHeaderButtonIdsBufferGroupList = new ByteArrayFilterGroupList();

    static {
        for (StringFilterGroup buttonId : pageHeaderButtonIds) {
            pageHeaderButtonIdsBufferGroupList.addAll(
                    new ByteArrayFilterGroup(buttonId.setting, buttonId.filters[0].toString())
            );
        }
    }

    /**
     * If the spacers between the channel page header buttons are hidden with the subscribe button.
     */
    private static volatile boolean hideChannelProfileHeaderSpacers;

    /**
     * Injection point.
     * Removes the hidden buttons from the data of the page header (You tab and channel page),
     * so the header lays out the other buttons without the empty space of the hidden buttons.
     */
    public static byte[] hidePageHeaderButtons(byte[] bytes) {
        try {
            if (!pageHeaderBuffer.check(bytes).isFiltered()) {
                return bytes;
            }

            hideChannelProfileHeaderSpacers = false;
            if (!Settings.HIDE_SUBSCRIBE_BUTTON_IN_CHANNEL_PAGE.get()
                    && !pageHeaderButtonIdsBufferGroupList.check(bytes).isFiltered()) {
                return bytes;
            }

            List<ProtoNode> element = ProtoNode.parse(bytes);
            if (element == null) {
                return bytes;
            }

            boolean modified = false;
            for (List<ProtoNode> buttons : findButtonLists(element)) {
                int visibleButtons = 0;
                boolean hasHiddenSubscribeButton = false;

                for (ProtoNode button : new ArrayList<>(buttons)) {
                    List<ProtoNode> fields = button.children;
                    if (fields == null) {
                        continue;
                    }

                    if (ProtoNode.field(fields, SUBSCRIBE_BUTTON_VIEW_MODEL_FIELD) != null) {
                        // The header keeps the space of the subscribe button if it's removed,
                        // so it's hidden by the Litho filter.
                        hasHiddenSubscribeButton = Settings.HIDE_SUBSCRIBE_BUTTON_IN_CHANNEL_PAGE.get();
                    } else {
                        if (ProtoNode.field(fields, BUTTON_VIEW_MODEL_FIELD) != null) {
                            if (isPageHeaderButtonHidden(fields)) {
                                button.remove();
                                modified = true;
                            } else {
                                visibleButtons++;
                            }
                        }
                    }
                }

                if (hasHiddenSubscribeButton) {
                    // The spacers are only needed with 2 or more visible buttons.
                    hideChannelProfileHeaderSpacers = visibleButtons < 2;
                }
            }

            return modified ? ProtoNode.write(element) : bytes;
        } catch (Exception ex) {
            Logger.printException(() -> "hidePageHeaderButtons failure", ex);
        }

        return bytes;
    }

    /**
     * @return The fields of the messages that hold the buttons,
     *         except the ones inside the buttons (such as menus).
     */
    private static List<List<ProtoNode>> findButtonLists(List<ProtoNode> element) {
        List<ProtoNode> buttonLists = new ArrayList<>();
        for (ProtoNode buttonViewModel : ProtoNode.findMessages(element, BUTTON_VIEW_MODEL_FIELD)) {
            ProtoNode button = buttonViewModel.getParent();
            ProtoNode buttons = button == null ? null : button.getParent();
            if (buttons != null && !buttonLists.contains(buttons)) {
                buttonLists.add(buttons);
            }
        }

        List<List<ProtoNode>> outerButtonLists = new ArrayList<>(buttonLists.size());
        for (ProtoNode buttons : buttonLists) {
            ProtoNode ancestor = buttons.getParent();
            while (ancestor != null && !buttonLists.contains(ancestor)) {
                ancestor = ancestor.getParent();
            }
            List<ProtoNode> fields = buttons.children;
            if (ancestor == null && fields != null) {
                outerButtonLists.add(fields);
            }
        }

        return outerButtonLists;
    }

    /**
     * @return If the button has the id of a hidden button, as the full text or after the id prefix.
     */
    private static boolean isPageHeaderButtonHidden(List<ProtoNode> button) {
        for (ProtoNode text : ProtoNode.textNodes(button)) {
            String value = text.getText();
            for (StringFilterGroup buttonId : pageHeaderButtonIds) {
                String id = buttonId.filters[0].toString();
                if (buttonId.isEnabled() && value.endsWith(id)
                        && (value.length() == id.length() || value.charAt(value.length() - id.length() - 1) == '.')) {
                    return true;
                }
            }
        }

        return false;
    }

}
