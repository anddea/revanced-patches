package app.revanced.extension.youtube.patches.components;

import androidx.annotation.Nullable;

import app.revanced.extension.shared.patches.components.ByteArrayFilterGroup;
import app.revanced.extension.shared.patches.components.Filter;
import app.revanced.extension.shared.patches.components.StringFilterGroup;
import app.revanced.extension.shared.patches.components.StringFilterGroupList;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.StringTrieSearch;
import app.revanced.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public final class FeedComponentsFilter extends Filter {
    private static final String CONVERSATION_CONTEXT_FEED_IDENTIFIER =
            "horizontalCollectionSwipeProtector=null";
    private static final String CONVERSATION_CONTEXT_SUBSCRIPTIONS_IDENTIFIER =
            "heightConstraint=null";
    private static final String INLINE_EXPANSION_PATH = "inline_expansion";
    private static final String FEED_VIDEO_PATH = "video_lockup_with_attachment";

    private static final ByteArrayFilterGroup inlineExpansion =
            new ByteArrayFilterGroup(
                    Settings.HIDE_EXPANDABLE_CHIP,
                    "inline_expansion"
            );

    private static final ByteArrayFilterGroup mixPlaylists =
            new ByteArrayFilterGroup(
                    null,
                    "&list="
            );
    private static final ByteArrayFilterGroup mixPlaylistsBufferExceptions =
            new ByteArrayFilterGroup(
                    null,
                    "cell_description_body",
                    "channel_profile"
            );
    private static final StringTrieSearch mixPlaylistsContextExceptions = new StringTrieSearch();

    private final StringFilterGroup channelProfile;
    private final StringFilterGroup communityPosts;
    private final StringFilterGroup expandableChip;
    private final ByteArrayFilterGroup visitStoreButton;
    private final StringFilterGroup videoLockup;

    private static final StringTrieSearch communityPostsFeedGroupSearch = new StringTrieSearch();
    private final StringFilterGroupList communityPostsFeedGroup = new StringFilterGroupList();


    public FeedComponentsFilter() {
        communityPostsFeedGroupSearch.addPatterns(
                CONVERSATION_CONTEXT_FEED_IDENTIFIER,
                CONVERSATION_CONTEXT_SUBSCRIPTIONS_IDENTIFIER
        );
        mixPlaylistsContextExceptions.addPatterns(
                "V.ED", // playlist browse id
                "java.lang.ref.WeakReference"
        );

        // Identifiers.

        final StringFilterGroup chipsShelf = new StringFilterGroup(
                Settings.HIDE_CHIPS_SHELF,
                "chips_shelf"
        );

        communityPosts = new StringFilterGroup(
                null,
                "post_base_wrapper",
                "images_post_root",
                "images_post_slim",
                "poll_post_root",
                "text_post_root"
        );

        final StringFilterGroup expandableShelf = new StringFilterGroup(
                Settings.HIDE_EXPANDABLE_SHELF,
                "expandable_section"
        );

        final StringFilterGroup feedSearchBar = new StringFilterGroup(
                Settings.HIDE_FEED_SEARCH_BAR,
                "search_bar_entry_point"
        );

        final StringFilterGroup tasteBuilder = new StringFilterGroup(
                Settings.HIDE_FEED_SURVEY,
                "selectable_item.eml",
                "cell_button.eml"
        );

        videoLockup = new StringFilterGroup(
                null,
                FEED_VIDEO_PATH
        );

        addIdentifierCallbacks(
                chipsShelf,
                communityPosts,
                expandableShelf,
                feedSearchBar,
                tasteBuilder,
                videoLockup
        );

        // Paths.

        final StringFilterGroup albumCard = new StringFilterGroup(
                Settings.HIDE_ALBUM_CARDS,
                "browsy_bar",
                "official_card"
        );

        channelProfile = new StringFilterGroup(
                Settings.HIDE_BROWSE_STORE_BUTTON,
                "channel_profile.eml",
                "page_header.eml" // new layout
        );

        visitStoreButton = new ByteArrayFilterGroup(
                null,
                "header_store_button"
        );

        final StringFilterGroup channelMemberShelf = new StringFilterGroup(
                Settings.HIDE_CHANNEL_MEMBER_SHELF,
                "member_recognition_shelf"
        );

        final StringFilterGroup channelProfileLinks = new StringFilterGroup(
                Settings.HIDE_CHANNEL_PROFILE_LINKS,
                "channel_header_links",
                "attribution.eml" // new layout
        );

        expandableChip = new StringFilterGroup(
                Settings.HIDE_EXPANDABLE_CHIP,
                INLINE_EXPANSION_PATH,
                "inline_expander",
                "expandable_metadata.eml"
        );

        final StringFilterGroup feedSurvey = new StringFilterGroup(
                Settings.HIDE_FEED_SURVEY,
                "feed_nudge",
                "_survey"
        );

        final StringFilterGroup forYouShelf = new StringFilterGroup(
                Settings.HIDE_FOR_YOU_SHELF,
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
                "horizontal_gaming_shelf.eml",
                "mini_game_card.eml"
        );

        final StringFilterGroup subscriptionsChannelBar = new StringFilterGroup(
                Settings.HIDE_SUBSCRIPTIONS_CAROUSEL,
                "subscriptions_channel_bar"
        );

        final StringFilterGroup ticketShelf = new StringFilterGroup(
                Settings.HIDE_TICKET_SHELF,
                "ticket_horizontal_shelf",
                "ticket_shelf"
        );

        addPathCallbacks(
                albumCard,
                channelProfile,
                channelMemberShelf,
                channelProfileLinks,
                expandableChip,
                feedSurvey,
                forYouShelf,
                imageShelf,
                latestPosts,
                movieShelf,
                notifyMe,
                playables,
                subscriptionsChannelBar,
                ticketShelf,
                videoLockup
        );

        final StringFilterGroup communityPostsHomeAndRelatedVideos =
                new StringFilterGroup(
                        Settings.HIDE_COMMUNITY_POSTS_HOME_RELATED_VIDEOS,
                        CONVERSATION_CONTEXT_FEED_IDENTIFIER
                );

        final StringFilterGroup communityPostsSubscriptions =
                new StringFilterGroup(
                        Settings.HIDE_COMMUNITY_POSTS_SUBSCRIPTIONS,
                        CONVERSATION_CONTEXT_SUBSCRIPTIONS_IDENTIFIER
                );

        communityPostsFeedGroup.addAll(communityPostsHomeAndRelatedVideos, communityPostsSubscriptions);
    }

    /**
     * Injection point.
     * <p>
     * Called from a different place then the other filters.
     */
    public static boolean filterMixPlaylists(final Object conversionContext, @Nullable final byte[] bytes) {
        try {
            if (!Settings.HIDE_MIX_PLAYLISTS.get()) {
                return false;
            }
            return bytes != null
                    && mixPlaylists.check(bytes).isFiltered()
                    && !mixPlaylistsBufferExceptions.check(bytes).isFiltered()
                    && !mixPlaylistsContextExceptions.matches(conversionContext.toString());
        } catch (Exception ex) {
            Logger.printException(() -> "filterMixPlaylists failure", ex);
        }

        return false;
    }

    @Override
    public boolean isFiltered(String path, @Nullable String identifier, String allValue, byte[] protobufBufferArray,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        if (matchedGroup == channelProfile) {
            if (contentIndex == 0 && visitStoreButton.check(protobufBufferArray).isFiltered()) {
                return super.isFiltered(path, identifier, allValue, protobufBufferArray, matchedGroup, contentType, contentIndex);
            }
            return false;
        } else if (matchedGroup == communityPosts) {
            if (!communityPostsFeedGroupSearch.matches(allValue) && Settings.HIDE_COMMUNITY_POSTS_CHANNEL.get()) {
                return super.isFiltered(path, identifier, allValue, protobufBufferArray, matchedGroup, contentType, contentIndex);
            }
            if (!communityPostsFeedGroup.check(allValue).isFiltered()) {
                return false;
            }
        } else if (matchedGroup == expandableChip) {
            if (path.startsWith(FEED_VIDEO_PATH)) {
                return super.isFiltered(path, identifier, allValue, protobufBufferArray, matchedGroup, contentType, contentIndex);
            }
            return false;
        } else if (matchedGroup == videoLockup) {
            if (contentIndex == 0 && path.startsWith("CellType|") && inlineExpansion.check(protobufBufferArray).isFiltered()) {
                return super.isFiltered(path, identifier, allValue, protobufBufferArray, matchedGroup, contentType, contentIndex);
            }
            return false;
        }

        return super.isFiltered(path, identifier, allValue, protobufBufferArray, matchedGroup, contentType, contentIndex);
    }
}
