package app.revanced.extension.youtube.patches.player;

import static app.revanced.extension.youtube.patches.player.ActionButtonsPatch.ActionButton.REMIX;

import androidx.annotation.Nullable;

import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import app.revanced.extension.shared.settings.BooleanSetting;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.Utils;
import app.revanced.extension.youtube.patches.player.requests.ActionButtonRequest;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.shared.VideoInformation;

@SuppressWarnings({"unused", "deprecation"})
public class ActionButtonsPatch {

    public enum ActionButton {
        UNKNOWN(
                null,
                null
        ),
        CLIP(
                "clipButtonViewModel",
                Settings.HIDE_CLIP_BUTTON
        ),
        DOWNLOAD(
                "downloadButtonViewModel",
                Settings.HIDE_DOWNLOAD_BUTTON
        ),
        LIKE_DISLIKE(
                "segmentedLikeDislikeButtonViewModel",
                Settings.HIDE_LIKE_DISLIKE_BUTTON
        ),
        LIVE_CHAT(
                "yt_outline_message_bubble",
                null
        ),
        PLAYLIST(
                "addToPlaylistButtonViewModel",
                Settings.HIDE_PLAYLIST_BUTTON
        ),
        REMIX(
                "yt_outline_youtube_shorts_plus",
                Settings.HIDE_REMIX_BUTTON
        ),
        REPORT(
                "yt_outline_flag",
                Settings.HIDE_REPORT_BUTTON
        ),
        REWARDS(
                "yt_outline_account_link",
                Settings.HIDE_REWARDS_BUTTON
        ),
        SHARE(
                "yt_outline_share",
                Settings.HIDE_SHARE_BUTTON
        ),
        SHOP(
                "yt_outline_bag",
                Settings.HIDE_SHOP_BUTTON
        ),
        THANKS(
                "yt_outline_dollar_sign_heart",
                Settings.HIDE_THANKS_BUTTON
        );

        @Nullable
        public final String identifier;
        @Nullable
        public final BooleanSetting setting;

        ActionButton(@Nullable String identifier, @Nullable BooleanSetting setting) {
            this.identifier = identifier;
            this.setting = setting;
        }
    }

    private static final String TARGET_COMPONENT_TYPE = "LazilyConvertedElement";
    private static final String VIDEO_ACTION_BAR_PATH_PREFIX = "video_action_bar.eml";
    private static final boolean HIDE_ACTION_BUTTON_INDEX = Settings.HIDE_ACTION_BUTTON_INDEX.get();
    private static final int REMIX_INDEX = Settings.REMIX_BUTTON_INDEX.get() - 1;

    /**
     * Injection point.
     */
    public static void fetchStreams(String url, Map<String, String> requestHeaders) {
        if (HIDE_ACTION_BUTTON_INDEX) {
            String id = Utils.getVideoIdFromRequest(url);
            if (id == null) {
                Logger.printException(() -> "Ignoring request with no id: " + url);
                return;
            } else if (id.isEmpty()) {
                return;
            }

            ActionButtonRequest.fetchRequestIfNeeded(id, requestHeaders);
        }
    }

    /**
     * Injection point.
     *
     * @param list       Type list of litho components
     * @param identifier Identifier of litho components
     */
    public static List<Object> hideActionButtonByIndex(@Nullable List<Object> list, @Nullable String identifier) {
        try {
            if (HIDE_ACTION_BUTTON_INDEX &&
                    identifier != null &&
                    identifier.startsWith(VIDEO_ACTION_BAR_PATH_PREFIX) &&
                    list != null &&
                    !list.isEmpty() &&
                    list.get(0).toString().equals(TARGET_COMPONENT_TYPE)
            ) {
                final int listSize = list.size();
                final String videoId = VideoInformation.getVideoId();
                ActionButtonRequest request = ActionButtonRequest.getRequestForVideoId(videoId);
                if (request != null) {
                    ActionButton[] actionButtons = request.getArray();
                    final int actionButtonsLength = actionButtons.length;
                    // The response is always included with the [LIKE_DISLIKE] button and the [SHARE] button.
                    // The minimum size of the action button array is 3.
                    if (actionButtonsLength > 2) {
                        // For some reason, the response does not contain the [REMIX] button.
                        // Add the [REMIX] button manually.
                        if (listSize - actionButtonsLength == 1) {
                            actionButtons = ArrayUtils.add(actionButtons, REMIX_INDEX, REMIX);
                        }
                        ActionButton[] finalActionButtons = actionButtons;
                        Logger.printDebug(() -> "videoId: " + videoId + ", buttons: " + Arrays.toString(finalActionButtons));
                        for (int i = actionButtons.length - 1; i > -1; i--) {
                            ActionButton actionButton = actionButtons[i];
                            if (actionButton.setting != null && actionButton.setting.get()) {
                                list.remove(i);
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Logger.printException(() -> "hideActionButtonByIndex failure", ex);
        }

        return list;
    }

}
