/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 */

package app.morphe.extension.youtube.patches.components;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;

import com.google.protobuf.MessageLite;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import app.morphe.extension.shared.patches.components.Filter;
import app.morphe.extension.shared.patches.components.StringFilterGroup;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.innertube.NextResponseOuterClass.ActionButtons;
import app.morphe.extension.youtube.innertube.NextResponseOuterClass.NewElement;
import app.morphe.extension.youtube.innertube.NextResponseOuterClass.SecondaryContents;
import app.morphe.extension.youtube.innertube.NextResponseOuterClass.SingleColumnWatchNextResults;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.shared.VideoInformation;

@SuppressWarnings("unused")
public final class ActionButtonsFilter extends Filter {
    public enum ActionButton {
        UNKNOWN(false),
        ASK(
                Settings.HIDE_ASK_BUTTON.get(),
                "yt_fill_experimental_spark",
                "yt_fill_spark"
        ),
        CHANNEL_PROFILE(false),
        CLIP(Settings.HIDE_CLIP_BUTTON.get()),
        COMMENTS(
                Settings.HIDE_COMMENTS_BUTTON.get(),
                "yt_outline_experimental_text_bubble",
                "yt_outline_message_bubble",
                "yt_outline_message_bubble_right"
        ),
        DOWNLOAD(Settings.HIDE_DOWNLOAD_BUTTON.get()),
        HYPE(
                Settings.HIDE_HYPE_BUTTON.get(),
                "yt_outline_experimental_hype",
                "yt_outline_star_shooting"
        ),
        LIKE_DISLIKE(Settings.HIDE_LIKE_DISLIKE_BUTTON.get()),
        PLAYLIST(Settings.HIDE_PLAYLIST_BUTTON.get()),
        PROMOTE(
                Settings.HIDE_PROMOTE_BUTTON.get(),
                "yt_outline_experimental_megaphone",
                "yt_outline_megaphone"
        ),
        REMIX(
                Settings.HIDE_REMIX_BUTTON.get(),
                "yt_outline_youtube_shorts_plus",
                "yt_outline_experimental_remix"
        ),
        REPORT(
                Settings.HIDE_REPORT_BUTTON.get(),
                "yt_outline_experimental_flag",
                "yt_outline_flag"
        ),
        REWARDS(
                Settings.HIDE_REWARDS_BUTTON.get(),
                "yt_outline_experimental_account_link",
                "yt_outline_account_link"
        ),
        SHARE(
                Settings.HIDE_SHARE_BUTTON.get(),
                "yt_outline_experimental_share",
                "yt_outline_share"
        ),
        SHOP(
                Settings.HIDE_SHOP_BUTTON.get(),
                "yt_outline_experimental_bag",
                "yt_outline_bag"
        ),
        STOP_ADS(
                Settings.HIDE_STOP_ADS_BUTTON.get(),
                "yt_outline_experimental_circle_slash",
                "yt_outline_slash_circle_left"
        ),
        THANKS(
                Settings.HIDE_THANKS_BUTTON.get(),
                "yt_outline_experimental_dollar_sign_heart",
                "yt_outline_dollar_sign_heart"
        );

        public final boolean shouldHide;
        @NonNull
        public final List<String> iconNames;

        ActionButton(boolean shouldHide) {
            this.shouldHide = shouldHide;
            this.iconNames = Collections.emptyList();
        }

        ActionButton(boolean shouldHide, @NonNull String... iconNames) {
            this.shouldHide = shouldHide;
            this.iconNames = Arrays.asList(iconNames);
        }
    }

    /**
     * Whether to parse watch next results and remove action buttons from the tree node list.
     */
    private static final boolean HIDE_ACTION_BUTTON;

    static {
        boolean hideActionButton = false;
        for (ActionButton button : ActionButton.values()) {
            if (button.shouldHide) {
                hideActionButton = true;
                break;
            }
        }
        HIDE_ACTION_BUTTON = !Settings.HIDE_ACTION_BUTTON_INDEX.get() && hideActionButton;
    }

    /**
     * Caches a list of action buttons based on video ID.
     */
    @GuardedBy("itself")
    private static final Map<String, List<ActionButton>> actionButtonLookup =
            Utils.createSizeRestrictedMap(10);

    private static final String COMPACT_CHANNEL_BAR_PREFIX = "compact_channel_bar.";
    private static final String COMPACTIFY_VIDEO_ACTION_BAR_PREFIX = "compactify_video_action_bar.";
    private static final String VIDEO_ACTION_BAR_PREFIX = "video_action_bar.";

    private final StringFilterGroup likeSubscribeGlow;

    public ActionButtonsFilter() {
        StringFilterGroup actionBarGroup = new StringFilterGroup(
                Settings.HIDE_ACTION_BAR,
                VIDEO_ACTION_BAR_PREFIX
        );
        addIdentifierCallbacks(actionBarGroup);

        likeSubscribeGlow = new StringFilterGroup(
                Settings.DISABLE_LIKE_DISLIKE_GLOW,
                "animated_button_border."
        );
        addPathCallbacks(likeSubscribeGlow);
    }

    private static boolean isVideoActionBar(@NonNull String identifier) {
        return StringUtils.startsWithAny(identifier, COMPACTIFY_VIDEO_ACTION_BAR_PREFIX, VIDEO_ACTION_BAR_PREFIX);
    }

    private static ActionButton getActionButton(@NonNull String iconName) {
        for (ActionButton button : ActionButton.values()) {
            for (String icon : button.iconNames) {
                if (iconName.contains(icon)) {
                    return button;
                }
            }
        }
        return ActionButton.UNKNOWN;
    }

    @Override
    public boolean isFiltered(String path, String identifier, String allValue, byte[] buffer,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        if (matchedGroup == likeSubscribeGlow) {
            return StringUtils.startsWithAny(
                    path,
                    COMPACT_CHANNEL_BAR_PREFIX,
                    COMPACTIFY_VIDEO_ACTION_BAR_PREFIX,
                    VIDEO_ACTION_BAR_PREFIX
            );
        }

        return true;
    }

    /**
     * Injection point.
     * Called after {@link #onSingleColumnWatchNextResultsLoaded(MessageLite)}.
     */
    public static void onLazilyConvertedElementLoaded(@NonNull List<Object> treeNodeResultList,
                                                      @NonNull String identifier) {
        if (!HIDE_ACTION_BUTTON || !isVideoActionBar(identifier)) {
            return;
        }

        synchronized (actionButtonLookup) {
            String videoId = VideoInformation.getVideoId();
            List<ActionButton> actionButtons = actionButtonLookup.get(videoId);
            if (actionButtons == null) {
                return;
            }

            int actionButtonSize = actionButtons.size();
            int treeNodeResultListSize = treeNodeResultList.size();
            if (actionButtonSize != treeNodeResultListSize) {
                Logger.printDebug(() -> "The sizes of the lists do not match, actionButtonSize: "
                        + actionButtonSize + ", treeNodeResultListSize: " + treeNodeResultListSize);
                return;
            }

            for (int i = actionButtonSize - 1; i > -1; i--) {
                ActionButton actionButton = actionButtons.get(i);
                if (actionButton.shouldHide && i < treeNodeResultListSize) {
                    treeNodeResultList.remove(i);
                }
            }
        }
    }

    /**
     * Injection point.
     * Invoke as soon as the endpoint response is received.
     */
    public static void onSingleColumnWatchNextResultsLoaded(@NonNull MessageLite messageLite) {
        if (!HIDE_ACTION_BUTTON) {
            return;
        }

        synchronized (actionButtonLookup) {
            try {
                var singleColumnWatchNextResults = SingleColumnWatchNextResults.parseFrom(messageLite.toByteArray());
                var primaryResults = singleColumnWatchNextResults.getPrimaryResults();
                var secondaryResults = primaryResults.getSecondaryResults();

                SecondaryContents finalSecondaryContents = null;
                for (var secondaryContents : secondaryResults.getSecondaryContentsList()) {
                    if (secondaryContents.hasSlimVideoMetadataSectionRenderer()) {
                        finalSecondaryContents = secondaryContents;
                        break;
                    }
                }
                if (finalSecondaryContents == null) {
                    return;
                }

                var slimVideoMetadataSectionRenderer = finalSecondaryContents.getSlimVideoMetadataSectionRenderer();
                String videoId = slimVideoMetadataSectionRenderer.getVideoId();
                if (actionButtonLookup.containsKey(videoId)) {
                    return;
                }

                NewElement finalNewElement = null;
                for (var tertiaryContents : slimVideoMetadataSectionRenderer.getTertiaryContentsList()) {
                    var newElement = tertiaryContents.getElementRenderer().getNewElement();
                    String identifier = newElement.getProperties().getIdentifierProperties().getIdentifier();
                    if (isVideoActionBar(identifier)) {
                        finalNewElement = newElement;
                        break;
                    }
                }
                if (finalNewElement == null) {
                    return;
                }

                var model = finalNewElement.getType().getComponentType().getModel();
                List<ActionButtons> finalActionButtons = null;

                if (model.hasYoutubeModel()) {
                    finalActionButtons = model.getYoutubeModel()
                            .getViewModel()
                            .getCompactifyVideoActionBarViewModel()
                            .getActionButtonsList();
                } else if (model.hasVideoActionBarModel()) {
                    finalActionButtons = model.getVideoActionBarModel()
                            .getVideoActionBarData()
                            .getActionButtonsList();
                } else {
                    Logger.printDebug(() -> "Unknown model: " + model + ", videoId: " + videoId);
                }
                if (finalActionButtons == null || finalActionButtons.isEmpty()) {
                    return;
                }

                List<ActionButton> actionButtons = new ArrayList<>(finalActionButtons.size());
                for (var buttons : finalActionButtons) {
                    ActionButton actionButton = ActionButton.UNKNOWN;
                    var primaryButtonViewModel = buttons.getPrimaryButtonViewModel();

                    if (primaryButtonViewModel.hasSecondaryButtonViewModel()) {
                        String iconName = primaryButtonViewModel.getSecondaryButtonViewModel().getIconName();
                        if (iconName != null) {
                            actionButton = getActionButton(iconName);
                            if (actionButton == ActionButton.UNKNOWN) {
                                Logger.printDebug(() -> "Unknown iconName: " + iconName + ", videoId: " + videoId);
                            }
                        }
                    } else if (primaryButtonViewModel.hasAddToPlaylistButtonViewModel()) {
                        actionButton = ActionButton.PLAYLIST;
                    } else if (primaryButtonViewModel.hasClipButtonViewModel()) {
                        actionButton = ActionButton.CLIP;
                    } else if (primaryButtonViewModel.hasCompactChannelBarViewModel()) {
                        actionButton = ActionButton.CHANNEL_PROFILE;
                    } else if (primaryButtonViewModel.hasDownloadButtonViewModel()) {
                        actionButton = ActionButton.DOWNLOAD;
                    } else if (primaryButtonViewModel.hasSegmentedLikeDislikeButtonViewModel()) {
                        actionButton = ActionButton.LIKE_DISLIKE;
                    } else {
                        Logger.printDebug(() -> "Unknown buttonViewModel: " + primaryButtonViewModel + ", videoId: " + videoId);
                    }

                    actionButtons.add(actionButton);
                }

                Logger.printDebug(() -> "New video id: " + videoId + ", action buttons: " + actionButtons);
                actionButtonLookup.put(videoId, actionButtons);
            } catch (Exception ex) {
                Logger.printException(() -> "Failed to parse SingleColumnWatchNextResults", ex);
            }
        }
    }
}
