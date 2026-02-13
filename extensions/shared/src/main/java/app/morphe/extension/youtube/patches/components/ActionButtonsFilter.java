package app.morphe.extension.youtube.patches.components;

import static app.morphe.extension.youtube.utils.ExtendedUtils.IS_19_26_OR_GREATER;

import org.apache.commons.lang3.StringUtils;

import app.morphe.extension.shared.patches.components.ByteArrayFilterGroup;
import app.morphe.extension.shared.patches.components.ByteArrayFilterGroupList;
import app.morphe.extension.shared.patches.components.Filter;
import app.morphe.extension.shared.patches.components.StringFilterGroup;
import app.morphe.extension.youtube.settings.Settings;

@SuppressWarnings({"deprecation", "unused"})
public final class ActionButtonsFilter extends Filter {
    /**
     * Video bar path when the video information is collapsed. Seems to shown only with 20.14+
     */
    private static final String COMPACTIFY_VIDEO_ACTION_BAR_PATH = "compactify_video_action_bar.";
    private static final String VIDEO_ACTION_BAR_PATH = "video_action_bar.";
    private static final String ANIMATED_VECTOR_TYPE_PATH = "AnimatedVectorType";

    private final StringFilterGroup actionBarRule;
    private final StringFilterGroup bufferFilterPathRule;
    private final StringFilterGroup likeSubscribeGlow;
    private final ByteArrayFilterGroupList bufferButtonsGroupList = new ByteArrayFilterGroupList();

    private static final boolean HIDE_ACTION_BUTTON_INDEX = Settings.HIDE_ACTION_BUTTON_INDEX.get();

    public ActionButtonsFilter() {
        actionBarRule = new StringFilterGroup(
                null,
                VIDEO_ACTION_BAR_PATH
        );
        addIdentifierCallbacks(actionBarRule);

        bufferFilterPathRule = new StringFilterGroup(
                null,
                "|ContainerType|button."
        );
        likeSubscribeGlow = new StringFilterGroup(
                Settings.DISABLE_LIKE_DISLIKE_GLOW,
                "animated_button_border."
        );
        addPathCallbacks(
                new StringFilterGroup(
                        Settings.HIDE_LIKE_DISLIKE_BUTTON,
                        "|segmented_like_dislike_button"
                ),
                new StringFilterGroup(
                        Settings.HIDE_DOWNLOAD_BUTTON,
                        "|download_button."
                ),
                new StringFilterGroup(
                        Settings.HIDE_CLIP_BUTTON,
                        "|clip_button."
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
                        Settings.HIDE_COMMENTS_BUTTON,
                        "yt_outline_message_bubble"
                ),
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
                        Settings.HIDE_THANKS_BUTTON,
                        "yt_outline_dollar_sign_heart"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_ASK_BUTTON,
                        "yt_fill_spark"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_SHOP_BUTTON,
                        "yt_outline_bag"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_STOP_ADS_BUTTON,
                        "yt_outline_slash_circle_left"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_CLIP_BUTTON,
                        "yt_outline_scissors"
                ),
                // 1. YouTube 19.25.39 can be used without the 'Disable update screen' patch.
                //    This means that even if you use an unpatched YouTube 19.25.39, the 'Update your app' pop-up will not appear.
                // 2. Due to a server-side change, the Hype button is now available on YouTube 19.25.39 and earlier.
                // 3. Google did not add the Hype icon (R.drawable.yt_outline_star_shooting_black_24) to YouTube 19.25.39 or earlier,
                //    So no icon appears on the Hype button when using YouTube 19.25.39.
                // 4. For the same reason, the 'buttonViewModel.iconName' value in the '/next' endpoint response from YouTube 19.25.39 is also empty.
                // 5. Therefore, in YouTube 19.25.39 or earlier versions, you cannot hide the Hype button with the keyword 'yt_outline_star_shooting',
                //    but you can hide it with the keyword 'Hype'.
                new ByteArrayFilterGroup(
                        Settings.HIDE_HYPE_BUTTON,
                        IS_19_26_OR_GREATER || Settings.FIX_HYPE_BUTTON_ICON.get()
                                ? "yt_outline_star_shooting"
                                : "Hype"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_PROMOTE_BUTTON,
                        "yt_outline_megaphone"
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
        if (HIDE_ACTION_BUTTON_INDEX) {
            return false;
        }
        if (!StringUtils.startsWithAny(path, COMPACTIFY_VIDEO_ACTION_BAR_PATH, VIDEO_ACTION_BAR_PATH)) {
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
            return bufferButtonsGroupList.check(buffer).isFiltered();
        }

        return true;
    }
}
