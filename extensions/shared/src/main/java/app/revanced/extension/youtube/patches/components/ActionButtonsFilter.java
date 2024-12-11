package app.revanced.extension.youtube.patches.components;

import androidx.annotation.Nullable;

import app.revanced.extension.shared.patches.components.ByteArrayFilterGroup;
import app.revanced.extension.shared.patches.components.ByteArrayFilterGroupList;
import app.revanced.extension.shared.patches.components.Filter;
import app.revanced.extension.shared.patches.components.StringFilterGroup;
import app.revanced.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public final class ActionButtonsFilter extends Filter {
    private static final String VIDEO_ACTION_BAR_PATH_PREFIX = "video_action_bar.eml";
    private static final String ANIMATED_VECTOR_TYPE_PATH = "AnimatedVectorType";

    private final StringFilterGroup actionBarRule;
    private final StringFilterGroup bufferFilterPathRule;
    private final StringFilterGroup likeSubscribeGlow;
    private final ByteArrayFilterGroupList bufferButtonsGroupList = new ByteArrayFilterGroupList();

    public ActionButtonsFilter() {
        actionBarRule = new StringFilterGroup(
                null,
                VIDEO_ACTION_BAR_PATH_PREFIX
        );
        addIdentifierCallbacks(actionBarRule);

        bufferFilterPathRule = new StringFilterGroup(
                null,
                "|ContainerType|button.eml|"
        );
        likeSubscribeGlow = new StringFilterGroup(
                Settings.DISABLE_LIKE_DISLIKE_GLOW,
                "animated_button_border.eml"
        );
        addPathCallbacks(
                new StringFilterGroup(
                        Settings.HIDE_LIKE_DISLIKE_BUTTON,
                        "|segmented_like_dislike_button"
                ),
                new StringFilterGroup(
                        Settings.HIDE_DOWNLOAD_BUTTON,
                        "|download_button.eml|"
                ),
                new StringFilterGroup(
                        Settings.HIDE_CLIP_BUTTON,
                        "|clip_button.eml|"
                ),
                new StringFilterGroup(
                        Settings.HIDE_PLAYLIST_BUTTON,
                        "|save_to_playlist_button"
                ),
                new StringFilterGroup(
                        Settings.HIDE_REWARDS_BUTTON,
                        "account_link_button"
                ),
                bufferFilterPathRule,
                likeSubscribeGlow
        );

        bufferButtonsGroupList.addAll(
                new ByteArrayFilterGroup(
                        Settings.HIDE_REPORT_BUTTON,
                        "yt_outline_flag"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_SHARE_BUTTON,
                        "yt_outline_share"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_REMIX_BUTTON,
                        "yt_outline_youtube_shorts_plus"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_SHOP_BUTTON,
                        "yt_outline_bag"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_THANKS_BUTTON,
                        "yt_outline_dollar_sign_heart"
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
        if (matchedGroup == likeSubscribeGlow) {
            if (!path.contains(ANIMATED_VECTOR_TYPE_PATH)) {
                return false;
            }
        }
        if (matchedGroup == bufferFilterPathRule) {
            // In case the group list has no match, return false.
            if (!bufferButtonsGroupList.check(protobufBufferArray).isFiltered()) {
                return false;
            }
        }

        return super.isFiltered(path, identifier, allValue, protobufBufferArray, matchedGroup, contentType, contentIndex);
    }
}
