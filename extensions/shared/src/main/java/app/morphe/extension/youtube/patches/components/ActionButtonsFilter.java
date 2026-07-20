/*
 * Copyright (C) 2024-2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
 * - inotia00 (https://github.com/inotia00)
 *
 * Licensed under the GNU General Public License v3.0.
 *
 * ------------------------------------------------------------------------
 * GPLv3 Section 7 – Additional Terms & Attribution Requirements
 * ------------------------------------------------------------------------
 *
 * This file contains substantial original work by the author(s) listed above.
 *
 * In accordance with Section 7 of the GNU General Public License v3.0,
 * the following additional terms apply to this file:
 *
 * 1. Source Credit Preservation (Section 7(b)): This specific copyright notice
 *    and the list of original authors above must be preserved in any copy
 *    or derivative work. You may add your own copyright notice below it,
 *    but you may not remove the original one.
 *
 * 2. Origin & Modification Marking (Section 7(c)): Modified versions must be
 *    clearly marked as such (e.g., by adding a "Modified by" line or a new
 *    copyright notice) and must not be misrepresented as the original work.
 *
 * 3. Version Control Attribution (Section 7(b)): Any ports or substantial
 *    modifications must retain historical authorship credit in version control
 *    systems (e.g., Git), listing original author(s) appropriately and
 *    modifiers as committers or co-authors.
 *
 * 4. User Interface Attribution (Section 7(b)): Any works containing or
 *    derived from this material must maintain a visible credit or
 *    acknowledgment to the original author(s) within the application's
 *    user interface (e.g., in an "About" or "Credits" section).
 */

/*
 * Portions of this file are ported from Morphe:
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 */

package app.morphe.extension.youtube.patches.components;

import static app.morphe.extension.youtube.utils.ExtendedUtils.IS_20_22_OR_GREATER;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;

import com.google.protobuf.MessageLite;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import app.morphe.extension.shared.patches.components.ByteArrayFilterGroup;
import app.morphe.extension.shared.patches.components.ByteArrayFilterGroupList;
import app.morphe.extension.shared.patches.components.Filter;
import app.morphe.extension.shared.patches.components.StringFilterGroup;
import app.morphe.extension.shared.patches.components.StringFilterGroupList;
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
        CONNECT(Settings.HIDE_CONNECT_BUTTON.get()),
        DISLIKE(Settings.HIDE_DISLIKE_BUTTON.get()),
        DOWNLOAD(Settings.HIDE_DOWNLOAD_BUTTON.get()),
        HYPE(
                Settings.HIDE_HYPE_BUTTON.get(),
                "yt_outline_experimental_hype",
                "yt_outline_star_shooting"
        ),
        LIKE(Settings.HIDE_LIKE_BUTTON.get()),
        LIKE_DISLIKE(Settings.HIDE_LIKE_DISLIKE_BUTTON.get()),
        MORE(Settings.HIDE_MORE_BUTTON.get()),
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
    private static final String SEGMENTED_LIKE_DISLIKE_BUTTON_PREFIX = "segmented_like_dislike_button.";
    private static final String VIDEO_ACTION_BAR_PREFIX = "video_action_bar.";
    private static final String ELEMENT_BUTTON_ID = "id.elements.button";
    private static final String MORE_BUTTON_PATH = "overflow_menu_button.e";

    private final StringFilterGroup actionBarGroup;
    private final StringFilterGroup likeSubscribeGlow;
    private final StringFilterGroup moreButton;
    private final StringFilterGroupList accessibilityGroupList = new StringFilterGroupList();
    private final ByteArrayFilterGroupList bufferGroupList = new ByteArrayFilterGroupList();

    public ActionButtonsFilter() {
        actionBarGroup = new StringFilterGroup(
                null,
                IS_20_22_OR_GREATER
                        ? new String[]{VIDEO_ACTION_BAR_PREFIX}
                        : new String[]{VIDEO_ACTION_BAR_PREFIX, COMPACTIFY_VIDEO_ACTION_BAR_PREFIX}
        );
        addIdentifierCallbacks(actionBarGroup);

        likeSubscribeGlow = new StringFilterGroup(
                Settings.DISABLE_LIKE_DISLIKE_GLOW,
                "animated_button_border."
        );
        moreButton = new StringFilterGroup(
                Settings.HIDE_MORE_BUTTON,
                MORE_BUTTON_PATH
        );
        addPathCallbacks(likeSubscribeGlow, moreButton);

        accessibilityGroupList.addAll(
                new StringFilterGroup(
                        Settings.HIDE_DISLIKE_BUTTON,
                        "id.video.dislike"
                ),
                new StringFilterGroup(
                        Settings.HIDE_LIKE_BUTTON,
                        "id.video.like"
                ),
                new StringFilterGroup(
                        Settings.HIDE_SHARE_BUTTON,
                        "id.video.share"
                ),
                new StringFilterGroup(
                        Settings.HIDE_PLAYLIST_BUTTON,
                        "id.video.add_to.button"
                )
        );
        bufferGroupList.addAll(
                new ByteArrayFilterGroup(
                        Settings.HIDE_ASK_BUTTON,
                        "PAyouchat"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_HYPE_BUTTON,
                        "yt_outline_experimental_hype",
                        "yt_outline_star_shooting"
                )
        );
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
        if (matchedGroup == moreButton) {
            return true;
        }
        if (matchedGroup == actionBarGroup) {
            if (Settings.HIDE_ACTION_BAR.get()) {
                return true;
            }
            if (Settings.HIDE_ACTION_BUTTON_INDEX.get()) {
                return false;
            }

            String accessibility = allValue == null ? "" : allValue;
            if (!path.contains(SEGMENTED_LIKE_DISLIKE_BUTTON_PREFIX)
                    && accessibilityGroupList.check(accessibility).isFiltered()) {
                return true;
            }
            // More menu entries and shared icon names are serialized into one component buffer.
            // Filtering that buffer would hide the entire More button instead of one menu entry.
            return !path.contains(MORE_BUTTON_PATH)
                    && accessibility.startsWith(ELEMENT_BUTTON_ID)
                    && bufferGroupList.check(buffer).isFiltered();
        }

        return true;
    }

    @Override
    public boolean isFiltered(Object contextSource, String identifier, String accessibility, String path, byte[] buffer,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        return isFiltered(path, identifier, accessibility, buffer, matchedGroup, contentType, contentIndex);
    }

    /**
     * v20.21 needs accessibility and the direct component buffer for buttons not represented by
     * the legacy action-button model.
     */
    @Override
    public boolean useModernFilterDataInLegacyBridge() {
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

    public static int getCurrentVideoActionButtonIndex(@NonNull ActionButton actionButton) {
        synchronized (actionButtonLookup) {
            List<ActionButton> actionButtons = actionButtonLookup.get(VideoInformation.getVideoId());
            if (actionButtons == null || actionButton.shouldHide) {
                return -1;
            }

            int visibleIndex = 0;
            for (ActionButton currentButton : actionButtons) {
                if (currentButton == actionButton) {
                    return visibleIndex;
                }
                if (!currentButton.shouldHide) {
                    visibleIndex++;
                }
            }
            return -1;
        }
    }

    /**
     * Returns whether the unfiltered watch-next model contains an action button. Unlike the
     * visible index, this can identify the segmented layout even when that button is hidden.
     */
    public static boolean hasCurrentVideoActionButton(@NonNull ActionButton actionButton) {
        synchronized (actionButtonLookup) {
            List<ActionButton> actionButtons = actionButtonLookup.get(VideoInformation.getVideoId());
            return actionButtons != null && actionButtons.contains(actionButton);
        }
    }

    public static int getCurrentVideoActionButtonCount() {
        synchronized (actionButtonLookup) {
            List<ActionButton> actionButtons = actionButtonLookup.get(VideoInformation.getVideoId());
            if (actionButtons == null) {
                return -1;
            }

            int visibleButtonCount = 0;
            for (ActionButton actionButton : actionButtons) {
                if (!actionButton.shouldHide) {
                    visibleButtonCount++;
                }
            }
            return visibleButtonCount;
        }
    }

    /**
     * Injection point.
     * Invoke as soon as the endpoint response is received.
     */
    public static void onSingleColumnWatchNextResultsLoaded(@NonNull MessageLite messageLite) {
        if (!HIDE_ACTION_BUTTON && !Settings.RYD_ENABLED.get()) {
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
                    } else if (primaryButtonViewModel.hasAccountLinkButtonViewModel()) {
                        actionButton = ActionButton.CONNECT;
                    } else if (primaryButtonViewModel.hasAddToPlaylistButtonViewModel()) {
                        actionButton = ActionButton.PLAYLIST;
                    } else if (primaryButtonViewModel.hasClipButtonViewModel()) {
                        actionButton = ActionButton.CLIP;
                    } else if (primaryButtonViewModel.hasCompactChannelBarViewModel()) {
                        actionButton = ActionButton.CHANNEL_PROFILE;
                    } else if (primaryButtonViewModel.hasDislikeButtonViewModel()) {
                        actionButton = ActionButton.DISLIKE;
                    } else if (primaryButtonViewModel.hasDownloadButtonViewModel()) {
                        actionButton = ActionButton.DOWNLOAD;
                    } else if (primaryButtonViewModel.hasLikeButtonViewModel()) {
                        actionButton = ActionButton.LIKE;
                    } else if (primaryButtonViewModel.hasOverflowMenuButtonViewModel()) {
                        actionButton = ActionButton.MORE;
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
