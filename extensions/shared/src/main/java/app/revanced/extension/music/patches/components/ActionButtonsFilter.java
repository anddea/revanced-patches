package app.revanced.extension.music.patches.components;

import androidx.annotation.Nullable;

import app.revanced.extension.music.settings.Settings;
import app.revanced.extension.shared.patches.components.ByteArrayFilterGroup;
import app.revanced.extension.shared.patches.components.ByteArrayFilterGroupList;
import app.revanced.extension.shared.patches.components.Filter;
import app.revanced.extension.shared.patches.components.StringFilterGroup;

@SuppressWarnings("unused")
public final class ActionButtonsFilter extends Filter {
    private static final String VIDEO_ACTION_BAR_PATH_PREFIX = "video_action_bar.eml";

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
                "|ContainerType|button.eml"
        );
        final StringFilterGroup downloadButton = new StringFilterGroup(
                Settings.HIDE_ACTION_BUTTON_DOWNLOAD,
                "music_download_button.eml"
        );
        final StringFilterGroup likeDislikeContainer = new StringFilterGroup(
                Settings.HIDE_ACTION_BUTTON_LIKE_DISLIKE,
                "segmented_like_dislike_button.eml"
        );
        final StringFilterGroup songVideoButton = new StringFilterGroup(
                Settings.HIDE_ACTION_BUTTON_SONG_VIDEO,
                "music_audio_video_button.eml"
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
    public boolean isFiltered(String path, @Nullable String identifier, String allValue, byte[] protobufBufferArray,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        if (!path.startsWith(VIDEO_ACTION_BAR_PATH_PREFIX)) {
            return false;
        }
        if (matchedGroup == actionBarRule && !isEveryFilterGroupEnabled()) {
            return false;
        }
        if (matchedGroup == bufferFilterPathRule && !bufferButtonsGroupList.check(protobufBufferArray).isFiltered()) {
            return false;
        }

        return super.isFiltered(path, identifier, allValue, protobufBufferArray, matchedGroup, contentType, contentIndex);
    }
}
