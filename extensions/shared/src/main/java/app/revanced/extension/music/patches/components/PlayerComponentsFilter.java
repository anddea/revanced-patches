package app.revanced.extension.music.patches.components;

import androidx.annotation.Nullable;

import app.revanced.extension.music.settings.Settings;
import app.revanced.extension.shared.patches.components.Filter;
import app.revanced.extension.shared.patches.components.StringFilterGroup;

@SuppressWarnings({"FieldCanBeLocal", "unused"})
public final class PlayerComponentsFilter extends Filter {
    private final String COMMENT_COMPOSER_PATH = "comment_composer";
    private final StringFilterGroup emojiPickerAndTimestamp;

    public PlayerComponentsFilter() {
        addIdentifierCallbacks(
                new StringFilterGroup(
                        Settings.HIDE_COMMENT_CHANNEL_GUIDELINES,
                        "channel_guidelines_entry_banner.eml",
                        "community_guidelines.eml"
                )
        );
        emojiPickerAndTimestamp = new StringFilterGroup(
                Settings.HIDE_COMMENT_TIMESTAMP_AND_EMOJI_BUTTONS,
                "|CellType|ContainerType|ContainerType|ContainerType|ContainerType|ContainerType|"
        );

        addPathCallbacks(emojiPickerAndTimestamp);
    }

    @Override
    public boolean isFiltered(String path, @Nullable String identifier, String allValue, byte[] protobufBufferArray,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        if (matchedGroup == emojiPickerAndTimestamp && !path.startsWith(COMMENT_COMPOSER_PATH)) {
            return false;
        }

        return super.isFiltered(path, identifier, allValue, protobufBufferArray, matchedGroup, contentType, contentIndex);
    }
}
