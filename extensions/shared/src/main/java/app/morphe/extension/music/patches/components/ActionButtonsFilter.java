package app.morphe.extension.music.patches.components;

import app.morphe.extension.music.settings.Settings;
import app.morphe.extension.shared.patches.components.ByteArrayFilterGroup;
import app.morphe.extension.shared.patches.components.ByteArrayFilterGroupList;
import app.morphe.extension.shared.patches.components.Filter;
import app.morphe.extension.shared.patches.components.StringFilterGroup;

@SuppressWarnings("unused")
public final class ActionButtonsFilter extends Filter {
    private static final String VIDEO_ACTION_BAR_PATH_PREFIX = "video_action_bar.";

    private final StringFilterGroup actionBarRule;
    private final StringFilterGroup bufferFilterPathRule;
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
                        "yt_outline_message_bubble"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_ACTION_BUTTON_ADD_TO_PLAYLIST,
                        "yt_outline_list_add"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_ACTION_BUTTON_SHARE,
                        "yt_outline_share"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_ACTION_BUTTON_RADIO,
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

    @Override
    public boolean isFiltered(String path, String identifier, String allValue, byte[] buffer,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        if (!path.startsWith(VIDEO_ACTION_BAR_PATH_PREFIX)) {
            return false;
        }
        if (matchedGroup == actionBarRule && !isEveryFilterGroupEnabled()) {
            return false;
        }
        return matchedGroup != bufferFilterPathRule || bufferButtonsGroupList.check(buffer).isFiltered();
    }
}
