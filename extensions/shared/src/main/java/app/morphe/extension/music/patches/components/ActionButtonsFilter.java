package app.morphe.extension.music.patches.components;

import app.morphe.extension.music.settings.Settings;
import app.morphe.extension.shared.patches.components.ByteArrayFilterGroup;
import app.morphe.extension.shared.patches.components.ByteArrayFilterGroupList;
import app.morphe.extension.shared.patches.components.Filter;
import app.morphe.extension.shared.patches.components.FilterGroup.FilterGroupResult;
import app.morphe.extension.shared.patches.components.StringFilterGroup;

@SuppressWarnings("unused")
public final class ActionButtonsFilter extends Filter {
    private static final String VIDEO_ACTION_BAR_PATH_PREFIX = "video_action_bar.";

    private final StringFilterGroup actionBarRule;
    private final StringFilterGroup bufferFilterPathRule;
    private final ByteArrayFilterGroup buttonContentEnd = new ByteArrayFilterGroup(
            null,
            "capabilities|",
            "sans-serif-regular",
            "yt_outline_overflow_vertical_white_24"
    );
    private final ByteArrayFilterGroupList bufferButtonsGroupList = new ByteArrayFilterGroupList();

    public ActionButtonsFilter() {
        actionBarRule = new StringFilterGroup(
                null,
                VIDEO_ACTION_BAR_PATH_PREFIX
        );
        addIdentifierCallbacks(actionBarRule);

        bufferFilterPathRule = new StringFilterGroup(
                null,
                "|ContainerType|button."
        );
        final StringFilterGroup downloadButton = new StringFilterGroup(
                Settings.HIDE_ACTION_BUTTON_DOWNLOAD,
                "music_download_button."
        );
        final StringFilterGroup likeDislikeContainer = new StringFilterGroup(
                Settings.HIDE_ACTION_BUTTON_LIKE_DISLIKE,
                "segmented_like_dislike_button."
        );
        final StringFilterGroup songVideoButton = new StringFilterGroup(
                Settings.HIDE_ACTION_BUTTON_SONG_VIDEO,
                "music_audio_video_button."
        );
        addPathCallbacks(
                bufferFilterPathRule,
                downloadButton,
                likeDislikeContainer,
                songVideoButton
        );

        bufferButtonsGroupList.addAll(
                new ByteArrayFilterGroup(
                        Settings.HIDE_ACTION_BUTTON_COMMENT,
                        "yt_outline_experimental_text_bubble_vd_theme_24",
                        "yt_outline_message_bubble"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_ACTION_BUTTON_ADD_TO_PLAYLIST,
                        "yt_outline_experimental_playlist_add_vd_theme_24",
                        "yt_outline_list_add"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_ACTION_BUTTON_LYRICS,
                        "yt_outline_experimental_quote_vd_theme_24"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_ACTION_BUTTON_SHARE,
                        "yt_outline_experimental_share_vd_theme_24",
                        "yt_outline_share"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_ACTION_BUTTON_RADIO,
                        "yt_outline_experimental_mix_vd_theme_24",
                        "yt_outline_youtube_mix"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_ACTION_BUTTON_DISABLED,
                        "button_container_disabled"
                )
        );
    }

    private boolean isEveryFilterGroupEnabled() {
        for (StringFilterGroup group : pathCallbacks)
            if (!group.isEnabled()) return false;

        for (ByteArrayFilterGroup group : bufferButtonsGroupList)
            if (!group.isEnabled()) return false;

        return true;
    }

    /**
     * Checks only the button-local portion of the component buffer.
     *
     * <p>Music appends shared component data after the button-local data. Matching an icon in the
     * shared data would make one enabled hide setting match unrelated buttons.</p>
     */
    private boolean matchesButtonContent(byte[] buffer) {
        final FilterGroupResult buttonMatch = bufferButtonsGroupList.check(buffer);
        if (!buttonMatch.isFiltered()) {
            return false;
        }

        final int contentEnd = buttonContentEnd.check(buffer).getMatchedIndex();
        return contentEnd < 0 || buttonMatch.getMatchedIndex() < contentEnd;
    }

    /**
     * New Litho action buttons are direct collection children. Removing the child prevents the
     * empty replacement component from retaining its horizontal layout slot.
     */
    public boolean removeFromComponentList() {
        return true;
    }

    @Override
    public boolean isFiltered(String path, String identifier, String allValue, byte[] buffer,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        if (!path.startsWith(VIDEO_ACTION_BAR_PATH_PREFIX)) {
            return false;
        }
        if (matchedGroup == actionBarRule && !isEveryFilterGroupEnabled()) {
            return false;
        }
        return matchedGroup != bufferFilterPathRule || matchesButtonContent(buffer);
    }
}
