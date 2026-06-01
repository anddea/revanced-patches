package app.morphe.extension.youtube.patches.components;

import app.morphe.extension.shared.patches.components.ByteArrayFilterGroup;
import app.morphe.extension.shared.patches.components.ByteArrayFilterGroupList;
import app.morphe.extension.shared.patches.components.Filter;
import app.morphe.extension.shared.patches.components.StringFilterGroup;
import app.morphe.extension.shared.utils.StringTrieSearch;
import app.morphe.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public final class QuickActionFilter extends Filter {
    private static final String QUICK_ACTION_PATH = "quick_actions.e";
    private final StringTrieSearch bufferFilterPathExceptions = new StringTrieSearch();
    private final StringFilterGroup quickActionRule;

    private final StringFilterGroup bufferFilterPathRule;
    private final ByteArrayFilterGroupList bufferButtonsGroupList = new ByteArrayFilterGroupList();

    private final StringFilterGroup liveChatReplay;

    public QuickActionFilter() {
        quickActionRule = new StringFilterGroup(
                Settings.HIDE_QUICK_ACTIONS,
                QUICK_ACTION_PATH
        );
        addIdentifierCallbacks(quickActionRule);
        bufferFilterPathRule = new StringFilterGroup(
                null,
                "|ContainerType|button.e",
                "|fullscreen_video_action_button.e"
        );

        liveChatReplay = new StringFilterGroup(
                Settings.HIDE_LIVE_CHAT_REPLAY_BUTTON,
                "live_chat_ep_entrypoint."
        );

        addIdentifierCallbacks(liveChatReplay);

        bufferFilterPathExceptions.addPatterns(
                "|like_button",
                "|dislike_button",
                "|comments_entry_point_button",
                "|save_to_playlist_button",
                "|overflow_menu_button",
                "|fullscreen_related_videos"
        );

        addPathCallbacks(
                new StringFilterGroup(
                        Settings.HIDE_QUICK_ACTIONS_LIKE_BUTTON,
                        "|like_button"
                ),
                new StringFilterGroup(
                        Settings.HIDE_QUICK_ACTIONS_DISLIKE_BUTTON,
                        "|dislike_button"
                ),
                new StringFilterGroup(
                        Settings.HIDE_QUICK_ACTIONS_COMMENT_BUTTON,
                        "|comments_entry_point_button"
                ),
                new StringFilterGroup(
                        Settings.HIDE_QUICK_ACTIONS_SAVE_TO_PLAYLIST_BUTTON,
                        "|save_to_playlist_button"
                ),
                new StringFilterGroup(
                        Settings.HIDE_QUICK_ACTIONS_MORE_BUTTON,
                        "|overflow_menu_button"
                ),
                new StringFilterGroup(
                        Settings.HIDE_RELATED_VIDEOS_OVERLAY,
                        "|fullscreen_related_videos"
                ),
                bufferFilterPathRule
        );

        bufferButtonsGroupList.addAll(
                new ByteArrayFilterGroup(
                        Settings.HIDE_QUICK_ACTIONS_COMMENT_BUTTON,
                        "yt_outline_experimental_text_bubble",
                        "yt_outline_message_bubble"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_QUICK_ACTIONS_LIVE_CHAT_BUTTON,
                        "yt_outline_experimental_bubble_stack",
                        "yt_outline_message_bubble_overlap"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_QUICK_ACTIONS_OPEN_MIX_PLAYLIST_BUTTON,
                        "yt_outline_experimental_mix",
                        "yt_outline_youtube_mix"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_QUICK_ACTIONS_OPEN_PLAYLIST_BUTTON,
                        "yt_outline_experimental_playlist",
                        "yt_outline_list_play_arrow"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_QUICK_ACTIONS_SHARE_BUTTON,
                        "yt_outline_experimental_share",
                        "yt_outline_share"
                )
        );
    }

    @Override
    public boolean isFiltered(String path, String identifier, String allValue, byte[] buffer,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        if (matchedGroup == liveChatReplay) {
            return true;
        }
        if (!path.startsWith(QUICK_ACTION_PATH)) {
            return false;
        }
        if (matchedGroup == quickActionRule) {
            return true;
        }
        if (matchedGroup == bufferFilterPathRule) {
            if (bufferFilterPathExceptions.matches(path)) {
                return false;
            }
            return bufferButtonsGroupList.check(buffer).isFiltered();
        }

        return true;
    }
}
