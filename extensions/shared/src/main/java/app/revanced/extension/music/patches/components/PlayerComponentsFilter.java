package app.revanced.extension.music.patches.components;

import app.revanced.extension.music.settings.Settings;
import app.revanced.extension.shared.patches.components.Filter;
import app.revanced.extension.shared.patches.components.StringFilterGroup;

@SuppressWarnings({"FieldCanBeLocal", "unused"})
public final class PlayerComponentsFilter extends Filter {
    private final String COMMENT_COMPOSER_PATH = "comment_composer";
    private final String LYRICS_SHARE_BUTTON_PATH = "|CellType|ContainerType|button.eml";
    private final StringFilterGroup emojiPickerAndTimestamp;
    private final StringFilterGroup lyricsShare;

    public PlayerComponentsFilter() {
        final StringFilterGroup channelGuidelines = new StringFilterGroup(
                Settings.HIDE_COMMENT_CHANNEL_GUIDELINES,
                "channel_guidelines_entry_banner.eml",
                "community_guidelines.eml"
        );
        lyricsShare = new StringFilterGroup(
                Settings.HIDE_LYRICS_SHARE_BUTTON,
                "timed_lyrics.eml"
        );

        addIdentifierCallbacks(channelGuidelines, lyricsShare);

        emojiPickerAndTimestamp = new StringFilterGroup(
                Settings.HIDE_COMMENT_TIMESTAMP_AND_EMOJI_BUTTONS,
                "|CellType|ContainerType|ContainerType|ContainerType|ContainerType|ContainerType|"
        );

        addPathCallbacks(emojiPickerAndTimestamp);
    }

    @Override
    public boolean isFiltered(String path, String identifier, String allValue, byte[] buffer,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        if (matchedGroup == emojiPickerAndTimestamp && !path.startsWith(COMMENT_COMPOSER_PATH)) {
            return false;
        } else if (matchedGroup == lyricsShare && !path.contains(LYRICS_SHARE_BUTTON_PATH)) {
            return false;
        }

        return true;
    }
}
