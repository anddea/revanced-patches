package app.morphe.extension.youtube.patches.components;

import app.morphe.extension.shared.patches.components.ByteArrayFilterGroup;
import app.morphe.extension.shared.patches.components.ByteArrayFilterGroupList;
import app.morphe.extension.shared.patches.components.Filter;
import app.morphe.extension.shared.patches.components.StringFilterGroup;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.shared.EngagementPanel;
import app.morphe.extension.youtube.shared.RootView;

@SuppressWarnings("unused")
public final class DescriptionsFilter extends Filter {
    private static final String INFOCARDS_SECTION_PATH = "infocards_section.";

    private final ByteArrayFilterGroupList featuredSectionGroupList = new ByteArrayFilterGroupList();
    private final ByteArrayFilterGroupList macroMarkerShelfGroupList = new ByteArrayFilterGroupList();
    private final ByteArrayFilterGroupList playlistSectionGroupList = new ByteArrayFilterGroupList();

    private final StringFilterGroup featuredSection;
    private final StringFilterGroup hashtagSection;
    private final ByteArrayFilterGroupList hashtagSectionGroupList = new ByteArrayFilterGroupList();
    private final StringFilterGroup howThisWasMadeSection;
    private final StringFilterGroup horizontalShelf;
    private final StringFilterGroup infoCardsSection;
    private final StringFilterGroup macroMarkerShelf;
    private final StringFilterGroup playlistSection;
    private final StringFilterGroup shortsHowThisWasMadeSection;
    private final StringFilterGroup subscribeButton;
    private final StringFilterGroup videoDetails;
    private final ByteArrayFilterGroup videoDetailsBuffer;

    public DescriptionsFilter() {
        final StringFilterGroup askSection = new StringFilterGroup(
                Settings.HIDE_ASK_SECTION,
                "input_composer_button.",
                "youchat_entrypoint."
        );

        final StringFilterGroup attributesSection = new StringFilterGroup(
                Settings.HIDE_ATTRIBUTES_SECTION,
                "video_attributes_section.",
                // It appears to be deprecated, but it can still be used in YouTube 19.05.36.
                "gaming_section.",
                "music_section.",
                "place_section."
        );

        // Keep this identifier callback for older YouTube versions. Newer versions expose the
        // same component through the path callback below, where the buffer can distinguish courses
        // from podcasts.
        final StringFilterGroup podcastSection = new StringFilterGroup(
                Settings.HIDE_EXPLORE_PODCAST_SECTION,
                "playlist_section."
        );

        final StringFilterGroup correctionsSection = new StringFilterGroup(
                Settings.HIDE_CORRECTIONS_SECTION,
                "error_corrections_section"
        );

        final StringFilterGroup courseProgressSection = new StringFilterGroup(
                Settings.HIDE_COURSE_PROGRESS_SECTION,
                "course_progress"
        );

        final StringFilterGroup transcriptSection = new StringFilterGroup(
                Settings.HIDE_TRANSCRIPT_SECTION,
                "transcript_section."
        );

        final StringFilterGroup videoSummarySection = new StringFilterGroup(
                Settings.HIDE_AI_GENERATED_VIDEO_SUMMARY_SECTION,
                "cell_expandable_metadata."
        );

        addIdentifierCallbacks(
                askSection,
                attributesSection,
                podcastSection,
                transcriptSection,
                videoSummarySection
        );

        featuredSection = new StringFilterGroup(
                null,
                "compact_infocard."
        );

        featuredSectionGroupList.addAll(
                new ByteArrayFilterGroup(
                        Settings.HIDE_FEATURED_CHANNELS_SECTION,
                        "structured_description_channel_lockup"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_FEATURED_LINKS_SECTION,
                        "media_lockup"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_FEATURED_PLAYLISTS_SECTION,
                        "structured_description_playlist_lockup"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_FEATURED_VIDEOS_SECTION,
                        "structured_description_video_lockup"
                )
        );

        hashtagSection = new StringFilterGroup(
                null,
                "|CellType|ScrollableContainerType|"
        );

        hashtagSectionGroupList.addAll(
                new ByteArrayFilterGroup(
                        Settings.HIDE_ATTRIBUTES_SECTION,
                        "yt_outline_location_point",
                        "yt_outline_experimental_location_pin"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_HASHTAG_SECTION,
                        "FEhashtag",
                        "/charts" // https://charts.youtube.com/charts/
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_HYPE_POINTS_SECTION,
                        "yt_outline_star_shooting",
                        "yt_fill_experimental_hype"
                )
        );

        howThisWasMadeSection = new StringFilterGroup(
                Settings.HIDE_HOW_THIS_WAS_MADE_SECTION,
                "how_this_was_made_section."
        );

        // In the latest YouTube, the Attribute section has the same path as the Carousel shelf.
        // To hide only the Attribute section, check if the Description panel is open.
        horizontalShelf = new StringFilterGroup(
                Settings.HIDE_ATTRIBUTES_SECTION,
                "horizontal_shelf."
        );

        final StringFilterGroup hypePointsSection = new StringFilterGroup(
                Settings.HIDE_HYPE_POINTS_SECTION,
                "hype_points_factoid."
        );

        infoCardsSection = new StringFilterGroup(
                null,
                "infocards_section."
        );

        macroMarkerShelf = new StringFilterGroup(
                null,
                "macro_markers_carousel."
        );

        macroMarkerShelfGroupList.addAll(
                new ByteArrayFilterGroup(
                        Settings.HIDE_CHAPTERS_SECTION,
                        "chapters_horizontal_shelf",
                        "auto-chapters",
                        "description-chapters"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_KEY_CONCEPTS_SECTION,
                        "learning_concept_macro_markers_carousel_shelf",
                        "learning-concept"
                )
        );

        final StringFilterGroup lensSection = new StringFilterGroup(
                Settings.HIDE_SEARCH_INSIDE_THIS_VIDEO_SECTION,
                "lens_section."
        );

        playlistSection = new StringFilterGroup(
                null,
                "playlist_section."
        );

        playlistSectionGroupList.addAll(
                new ByteArrayFilterGroup(
                        Settings.HIDE_EXPLORE_COURSE_SECTION,
                        "yt_outline_creator_academy",
                        "yt_outline_experimental_graduation_cap"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_EXPLORE_PODCAST_SECTION,
                        "FEpodcasts_destination",
                        "yt_outline_experimental_podcast"
                )
        );

        shortsHowThisWasMadeSection = new StringFilterGroup(
                Settings.HIDE_HOW_THIS_WAS_MADE_SECTION,
                "shelf_header.",
                "cell_video_attribute."
        );

        subscribeButton = new StringFilterGroup(
                Settings.HIDE_SUBSCRIBE_BUTTON,
                "subscribe_button."
        );

        videoDetails = new StringFilterGroup(
                null,
                "linear_layout."
        );

        videoDetailsBuffer = new ByteArrayFilterGroup(
                Settings.HIDE_VIDEO_DETAILS_SECTION,
                "section_header"
        );

        addPathCallbacks(
                howThisWasMadeSection,
                correctionsSection,
                courseProgressSection,
                featuredSection,
                hashtagSection,
                horizontalShelf,
                hypePointsSection,
                infoCardsSection,
                lensSection,
                macroMarkerShelf,
                playlistSection,
                shortsHowThisWasMadeSection,
                subscribeButton,
                videoDetails
        );
    }

    private boolean hideInfoCards(String path, int contentIndex) {
        if (contentIndex != 0) {
            return false;
        }

        final boolean hideInfoCardsSection = Settings.HIDE_INFO_CARDS_SECTION.get();
        final boolean hideFeaturedChannelsSection = Settings.HIDE_FEATURED_CHANNELS_SECTION.get();
        final boolean hideFeaturedLinksSection = Settings.HIDE_FEATURED_LINKS_SECTION.get();
        final boolean hideFeaturedPlaylistsSection = Settings.HIDE_FEATURED_PLAYLISTS_SECTION.get();
        final boolean hideFeaturedVideosSection = Settings.HIDE_FEATURED_VIDEOS_SECTION.get();
        final boolean hideSubscribeButton = Settings.HIDE_SUBSCRIBE_BUTTON.get();

        if (!hideInfoCardsSection && !hideFeaturedChannelsSection && !hideFeaturedLinksSection
                && !hideFeaturedPlaylistsSection && !hideFeaturedVideosSection && !hideSubscribeButton) {
            return false;
        }

        if (hideInfoCardsSection) {
            return true;
        }

        if (path.contains("structured_description_channel_lockup")) {
            return hideFeaturedChannelsSection;
        } else if (path.contains("media_lockup.")) {
            return hideFeaturedLinksSection;
        } else if (path.contains("structured_description_playlist_lockup")) {
            return hideFeaturedPlaylistsSection;
        } else if (path.contains("structured_description_video_lockup.")) {
            return hideFeaturedVideosSection;
        } else if (path.contains("subscribe_button.")) {
            return hideSubscribeButton;
        } else {
            return false;
        }
    }

    @Override
    public boolean isFiltered(String path, String identifier, String allValue, byte[] buffer,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        if (!EngagementPanel.isDescription() && !RootView.isPlayerActive() && !RootView.isShortsActive()) {
            return false;
        }

        // Check for the index because of likelihood of false positives.
        if (matchedGroup == howThisWasMadeSection) {
            return contentIndex == 0;
        } else if (matchedGroup == featuredSection) {
            return featuredSectionGroupList.check(buffer).isFiltered();
        } else if (matchedGroup == hashtagSection) {
            return hashtagSectionGroupList.check(buffer).isFiltered();
        } else if (matchedGroup == infoCardsSection) {
            return hideInfoCards(path, contentIndex);
        } else if (matchedGroup == macroMarkerShelf) {
            if (contentIndex != 0) {
                return false;
            }
            return macroMarkerShelfGroupList.check(buffer).isFiltered();
        } else if (matchedGroup == horizontalShelf) {
            if (contentIndex != 0) {
                return false;
            }
            if (!RootView.isPlayerActive()) {
                return false;
            }
            return EngagementPanel.isDescription();
        } else if (matchedGroup == playlistSection) {
            if (contentIndex != 0) {
                return false;
            }
            // YouTube 20.14.43 does not always include a buffer for these sections, so the
            // parent setting must also be checked independently of the subtype markers.
            return Settings.HIDE_EXPLORE_SECTION.get() || playlistSectionGroupList.check(buffer).isFiltered();
        } else if (matchedGroup == shortsHowThisWasMadeSection) {
            return RootView.isShortsActive() && EngagementPanel.isDescription();
        } else if (matchedGroup == subscribeButton) {
            return path.contains(INFOCARDS_SECTION_PATH);
        } else if (matchedGroup == videoDetails) {
            return videoDetailsBuffer.check(buffer).isFiltered();
        }

        return true;
    }
}
