package app.morphe.extension.youtube.patches.player;

import static app.morphe.extension.youtube.patches.player.ActionButtonsPatch.ActionButton.REMIX;
import static app.morphe.extension.youtube.utils.ExtendedUtils.IS_19_26_OR_GREATER;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.List;

import app.morphe.extension.shared.innertube.utils.AuthUtils;
import app.morphe.extension.shared.settings.BooleanSetting;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.youtube.patches.player.requests.ActionButtonRequest;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.shared.VideoInformation;

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
        COMMENTS(
                "yt_outline_message_bubble",
                Settings.HIDE_COMMENTS_BUTTON
        ),
        DOWNLOAD(
                "downloadButtonViewModel",
                Settings.HIDE_DOWNLOAD_BUTTON
        ),
        // 1. YouTube 19.25.39 can be used without the 'Disable update screen' patch.
        //    This means that even if you use an unpatched YouTube 19.25.39, the 'Update your app' pop-up will not appear.
        // 2. Due to a server-side change, the Hype button is now available on YouTube 19.25.39 and earlier.
        // 3. Google did not add the Hype icon (R.drawable.yt_outline_star_shooting_black_24) to YouTube 19.25.39 or earlier,
        //    So no icon appears on the Hype button when using YouTube 19.25.39.
        // 4. For the same reason, the 'buttonViewModel.iconName' value in the '/next' endpoint response from YouTube 19.25.39 is also empty.
        // 5. Therefore, in YouTube 19.25.39 or earlier versions, you cannot hide the Hype button with the keyword 'yt_outline_star_shooting',
        //    but you can hide it with the keyword 'Hype'.
        HYPE(
                IS_19_26_OR_GREATER
                        ? "yt_outline_star_shooting"
                        : "\"Hype\"",
                Settings.HIDE_HYPE_BUTTON
        ),
        LIKE_DISLIKE(
                "segmentedLikeDislikeButtonViewModel",
                Settings.HIDE_LIKE_DISLIKE_BUTTON
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

    private static final String VIDEO_ACTION_BAR_PATH_PREFIX = "video_action_bar.";
    private static final boolean HIDE_ACTION_BUTTON_INDEX = Settings.HIDE_ACTION_BUTTON_INDEX.get();
    private static final int REMIX_INDEX = Settings.REMIX_BUTTON_INDEX.get() - 1;

    /**
     * Injection point.
     */
    public static void fetchRequest(@NonNull String videoId, boolean isShortAndOpeningOrPlaying) {
        if (HIDE_ACTION_BUTTON_INDEX) {
            try {
                final boolean videoIdIsShort = VideoInformation.lastPlayerResponseIsShort();
                // Shorts shelf in home and subscription feed causes player response hook to be called,
                // and the 'is opening/playing' parameter will be false.
                // This hook will be called again when the Short is actually opened.
                if (videoIdIsShort && !isShortAndOpeningOrPlaying) {
                    return;
                }

                if (AuthUtils.isNotLoggedIn()) {
                    return;
                }

                ActionButtonRequest.fetchRequestIfNeeded(
                        videoId,
                        AuthUtils.getRequestHeader()
                );
            } catch (Exception ex) {
                Logger.printException(() -> "fetchRequest failure", ex);
            }
        }
    }

    /**
     * Injection point.
     *
     * @param list       Type list of litho components
     * @param identifier Identifier of litho components
     */
    public static void hideActionButtonByIndex(@NonNull List<Object> list, @NonNull String identifier) {
        try {
            if (HIDE_ACTION_BUTTON_INDEX &&
                    identifier.startsWith(VIDEO_ACTION_BAR_PATH_PREFIX)
            ) {
                final int listSize = list.size();
                final String videoId = VideoInformation.getVideoId();
                ActionButtonRequest request = ActionButtonRequest.getRequestForVideoId(videoId);
                if (request == null) {
                    return;
                }
                ActionButton[] actionButtons = request.getArray();
                final int actionButtonsLength = actionButtons.length;
                // The response is always included with the [LIKE_DISLIKE] button and the [SHARE] button.
                // The minimum size of the action button array is 3.
                if (actionButtonsLength < 3) {
                    return;
                }
                // For some reason, the response does not contain the [REMIX] button.
                // Add the [REMIX] button manually.
                if (listSize - actionButtonsLength == 1) {
                    actionButtons = ArrayUtils.add(actionButtons, REMIX_INDEX, REMIX);
                }
                ActionButton[] finalActionButtons = actionButtons;
                Logger.printDebug(() -> "videoId: " + videoId + ", buttons: " + Arrays.toString(finalActionButtons));
                for (int i = actionButtons.length - 1; i > -1; i--) {
                    ActionButton actionButton = actionButtons[i];
                    if (actionButton.setting != null &&
                            actionButton.setting.get() &&
                            i < listSize) {
                        list.remove(i);
                    }
                }
            }
        } catch (Exception ex) {
            Logger.printException(() -> "hideActionButtonByIndex failure", ex);
        }
    }

}
