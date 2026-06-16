package app.morphe.extension.youtube.patches.components;

import java.util.regex.Pattern;

import app.morphe.extension.shared.patches.components.ByteArrayFilterGroup;
import app.morphe.extension.shared.patches.components.Filter;
import app.morphe.extension.shared.patches.components.StringFilterGroup;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.StringTrieSearch;
import app.morphe.extension.youtube.innertube.NextResponseOuterClass.NewElement;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.shared.PlayerType;

@SuppressWarnings("unused")
public final class CommentsFilter extends Filter {
    private static final String COMMENT_COMPOSER_PATH = "comment_composer";
    private static final String COMMENT_ENTRY_POINT_TEASER_PATH = "comments_entry_point_teaser";
    private static final Pattern COMMENT_PREVIEW_TEXT_PATTERN = Pattern.compile("comments_entry_point_teaser.+ContainerType");
    private static final String FEED_VIDEO_PATH = "video_lockup_with_attachment";
    private static final String VIDEO_METADATA_CAROUSEL_PATH = "video_metadata_carousel.";

    private final StringFilterGroup chipBar;
    private final ByteArrayFilterGroup aiCommentsSummary;
    private final StringFilterGroup comments;
    private final StringFilterGroup commentsPreviewDots;
    private final StringFilterGroup createAShort;
    private final StringFilterGroup emojiPickerAndTimestamp;
    private final StringFilterGroup previewCommentText;
    private final StringFilterGroup thanks;
    private final StringTrieSearch exceptions = new StringTrieSearch();

    public CommentsFilter() {
        exceptions.addPatterns("macro_markers_list_item");

        final StringFilterGroup aiChatSummary = new StringFilterGroup(
                Settings.HIDE_AI_CHAT_SUMMARY,
                "live_chat_summary_banner"
        );

        chipBar = new StringFilterGroup(
                Settings.HIDE_AI_COMMENTS_SUMMARY,
                "chip_bar."
        );

        aiCommentsSummary = new ByteArrayFilterGroup(
                null,
                "yt_fill_spark"
        );

        final StringFilterGroup channelGuidelines = new StringFilterGroup(
                Settings.HIDE_CHANNEL_GUIDELINES,
                "channel_guidelines_entry_banner",
                "community_guidelines",
                "sponsorships_comments_upsell"
        );

        comments = new StringFilterGroup(
                null,
                VIDEO_METADATA_CAROUSEL_PATH,
                "comments_"
        );

        final StringFilterGroup commentsByMembers = new StringFilterGroup(
                Settings.HIDE_COMMENTS_BY_MEMBERS,
                "sponsorships_comments_header.",
                "sponsorships_comments_footer."
        );

        createAShort = new StringFilterGroup(
                Settings.HIDE_COMMENTS_CREATE_A_SHORT_BUTTON,
                "composer_short_creation_button"
        );

        emojiPickerAndTimestamp = new StringFilterGroup(
                Settings.HIDE_COMMENTS_EMOJI_AND_TIMESTAMP_BUTTONS,
                "|CellType|ContainerType|ContainerType|ContainerType|ContainerType|ContainerType|"
        );

        final StringFilterGroup liveChatMessages = new StringFilterGroup(
                Settings.HIDE_LIVE_CHAT_MESSAGES,
                "live_chat_text_message",
                "viewer_engagement_message" // message about poll, not poll itself
        );

        final StringFilterGroup previewComment = new StringFilterGroup(
                Settings.HIDE_PREVIEW_COMMENT_OLD_METHOD,
                "|carousel_item.",
                "|carousel_listener",
                COMMENT_ENTRY_POINT_TEASER_PATH,
                "comments_entry_point_simplebox"
        );

        commentsPreviewDots = new StringFilterGroup(
                Settings.HIDE_PREVIEW_COMMENT_OLD_METHOD,
                "|ContainerType|ContainerType|ContainerType|"
        );

        previewCommentText = new StringFilterGroup(
                Settings.HIDE_PREVIEW_COMMENT_NEW_METHOD,
                COMMENT_ENTRY_POINT_TEASER_PATH
        );

        thanks = new StringFilterGroup(
                Settings.HIDE_COMMENTS_THANKS_BUTTON,
                "|super_thanks_button."
        );


        addIdentifierCallbacks(channelGuidelines);

        addPathCallbacks(
                aiChatSummary,
                chipBar,
                comments,
                commentsByMembers,
                commentsPreviewDots,
                createAShort,
                emojiPickerAndTimestamp,
                liveChatMessages,
                previewComment,
                previewCommentText,
                thanks
        );
    }

    @Override
    public boolean isFiltered(String path, String identifier, String allValue, byte[] buffer,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        if (exceptions.matches(path))
            return false;

        if (matchedGroup == createAShort || matchedGroup == thanks || matchedGroup == emojiPickerAndTimestamp) {
            return path.startsWith(COMMENT_COMPOSER_PATH);
        } else if (matchedGroup == chipBar) {
            // Playlist sort button uses same components and must only filter if the player is opened.
            return PlayerType.getCurrent().isMaximizedOrFullscreen()
                    && aiCommentsSummary.check(buffer).isFiltered();
        } else if (matchedGroup == comments) {
            if (path.startsWith(FEED_VIDEO_PATH)) {
                return Settings.HIDE_COMMENTS_SECTION_IN_HOME_FEED.get();
            } else return Settings.HIDE_COMMENTS_SECTION.get();
        } else if (matchedGroup == commentsPreviewDots) {
            return path.startsWith(VIDEO_METADATA_CAROUSEL_PATH);
        } else if (matchedGroup == previewCommentText) {
            return COMMENT_PREVIEW_TEXT_PATTERN.matcher(path).find();
        }

        return true;
    }

    /**
     * Injection point.
     */
    public static byte[] onCommentsLoaded(byte[] bytes) {
        if (!Settings.HIDE_COMMENTS_CAROUSEL.get()) {
            return bytes;
        }

        String rawFilterText = Settings.HIDE_COMMENTS_CAROUSEL_FILTER_STRINGS.get();
        if (rawFilterText.isBlank()) {
            return bytes;
        }

        String[] commentsCarouselFilterStrings = rawFilterText.split("\\n");

        try {
            var newElement = NewElement.parseFrom(bytes).toBuilder();
            var identifier = newElement.getProperties().getIdentifierProperties().getIdentifier();
            if (!identifier.contains(VIDEO_METADATA_CAROUSEL_PATH)) {
                return bytes;
            }

            var type = newElement.getType().toBuilder();
            var componentType = type.getComponentType().toBuilder();
            var model = componentType.getModel().toBuilder();
            var videoMetadataCarouselModel = model.getVideoMetadataCarouselModel().toBuilder();
            var data = videoMetadataCarouselModel.getData().toBuilder();
            var carouselTitleDatasList = data.getCarouselTitleDatasList();
            boolean modified = false;

            for (int i = carouselTitleDatasList.size() - 1; i > -1; i--) {
                String title = carouselTitleDatasList.get(i).getTitle();
                Logger.printDebug(() -> "comments title: " + title);

                for (String filter : commentsCarouselFilterStrings) {
                    if (!filter.isEmpty() && title.contains(filter)) {
                        data.removeCarouselItemDatas(i);
                        data.removeCarouselTitleDatas(i);
                        modified = true;
                        break;
                    }
                }
            }

            if (modified) {
                videoMetadataCarouselModel.clearData();
                videoMetadataCarouselModel.setData(data.build());

                model.clearVideoMetadataCarouselModel();
                model.setVideoMetadataCarouselModel(videoMetadataCarouselModel.build());

                componentType.clearModel();
                componentType.setModel(model.build());

                type.clearComponentType();
                type.setComponentType(componentType.build());

                newElement.clearType();
                newElement.setType(type.build());

                return newElement.build().toByteArray();
            }
        } catch (Exception ex) {
            Logger.printException(() -> "Failed to parse newElement", ex);
        }

        return bytes;
    }
}
