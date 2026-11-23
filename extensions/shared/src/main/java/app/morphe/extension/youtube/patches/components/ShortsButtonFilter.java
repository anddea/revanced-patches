/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.extension.youtube.patches.components;

import static app.morphe.extension.youtube.utils.ExtendedUtils.IS_20_22_OR_GREATER;

import org.apache.commons.lang3.StringUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.protobuf.MessageLite;
import java.util.List;
import app.morphe.extension.youtube.patches.utils.ProtoNode;
import com.facebook.litho.ComponentHost;
import com.facebook.litho.TextContent;
import java.util.ArrayDeque;
import app.morphe.extension.shared.utils.Logger;

import app.morphe.extension.shared.patches.components.ByteArrayFilterGroup;
import app.morphe.extension.shared.patches.components.ByteArrayFilterGroupList;
import app.morphe.extension.shared.patches.components.Filter;
import app.morphe.extension.shared.patches.components.StringFilterGroup;
import app.morphe.extension.shared.patches.components.StringFilterGroupList;
import app.morphe.extension.youtube.settings.Settings;

@SuppressWarnings({"deprecation", "unused"})
public final class ShortsButtonFilter extends Filter {
    /**
     * If the Short whose page is created shows the bar below it, such as the comment bar.
     */
    private static volatile boolean shortShowsBottomActionBar;

    /**
     * Injection point.
     * Called with the configuration of a Shorts page right before its navigation bar mode is read.
     */
    public static void setReelWatchConfig(@Nullable MessageLite config) {
        boolean showsBottomActionBar = false;
        if (Settings.HIDE_SHORTS_PREVIEW_COMMENT.get() && config != null) {
            try {
                List<ProtoNode> fields = ProtoNode.parse(config.toByteArray());
                if (fields != null) {
                    // Field 8 holds the bar below the Short, its field 1 turns the bar on.
                    ProtoNode enabled = ProtoNode.field(ProtoNode.message(fields, 8), 1);
                    showsBottomActionBar = enabled != null && Long.valueOf(1).equals(enabled.getVarint());
                }
            } catch (Exception ex) {
                Logger.printException(() -> "setReelWatchConfig failure", ex);
            }
        }
        shortShowsBottomActionBar = showsBottomActionBar;
    }

    /**
     * Injection point.
     * <p>
     * The Shorts opened from a channel show the comment bar below them instead of the navigation bar.
     * When the comment bar is hidden, the navigation bar is kept.
     *
     * @param original The navigation bar mode: 1 shows it, 2 hides it,
     *                 3 hides it if the Short fits the screen without it.
     */
    public static int getNavigationBarMode(int original) {
        if (shortShowsBottomActionBar && (original == 2 || original == 3)) {
            return 1;
        }
        return original;
    }


    /**
     * Injection point.
     * <p>
     * The container of the bar below a Short keeps its height when the bar is hidden,
     * such as the comment bar hidden by "Hide preview comment", so the Short is shown above an empty bar.
     * The container is hidden while it shows nothing.
     */
    public static void hideEmptyBottomActionBar(View container) {
        try {
            if (!Settings.HIDE_SHORTS_PREVIEW_COMMENT.get() || container == null) {
                return;
            }
            ViewTreeObserver.OnPreDrawListener listener = () -> {
                try {
                    if (container.getVisibility() != View.VISIBLE) {
                        return true;
                    }
                    // A text or a view other than an empty Litho host, such as the bar of another feature.
                    // A Litho host without children or texts is what is left of a hidden bar.
                    ArrayDeque<View> views = new ArrayDeque<>();
                    views.add(container);
                    while (!views.isEmpty()) {
                        View view = views.poll();
                        if (view.getVisibility() != View.VISIBLE) {
                            continue;
                        }
                        if (view instanceof ComponentHost host) {
                            TextContent textContent = host.getTextContent();
                            if (textContent != null && !textContent.getTextItems().isEmpty()) {
                                return true;
                            }
                        } else if (!(view instanceof ViewGroup)) {
                            return true;
                        }
                        if (view instanceof ViewGroup group) {
                            for (int i = 0, count = group.getChildCount(); i < count; i++) {
                                views.add(group.getChildAt(i));
                            }
                        }
                    }
                    container.setVisibility(View.GONE);
                    // Not drawn with the empty bar.
                    return false;
                } catch (Exception ex) {
                    Logger.printException(() -> "Bottom action bar check failure", ex);
                }
                return true;
            };
            container.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(@NonNull View view) {
                    view.getViewTreeObserver().addOnPreDrawListener(listener);
                }

                @Override
                public void onViewDetachedFromWindow(@NonNull View view) {
                    view.getViewTreeObserver().removeOnPreDrawListener(listener);
                }
            });
            if (container.isAttachedToWindow()) {
                container.getViewTreeObserver().addOnPreDrawListener(listener);
            }
        } catch (Exception ex) {
            Logger.printException(() -> "hideEmptyBottomActionBar failure", ex);
        }
    }


    private static final String REEL_CHANNEL_BAR_PATH = "reel_channel_bar.";
    private static final String REEL_LIVE_HEADER_PATH = "immersive_live_header.";
    /**
     * For paid promotion label and subscribe button that appears in the channel bar.
     */
    private static final String REEL_METAPANEL_PATH = "reel_metapanel.e";
    private static final String REEL_PLAYER_OVERLAY_PATH = "reel_player_overlay.e";

    private static final String SHORTS_PAUSED_STATE_BUTTON_PATH = "|ScrollableContainerType|ContainerType|button.e";

    private final StringFilterGroup autoDubbedLabel;
    private final StringFilterGroup subscribeButton;
    private final StringFilterGroup joinButton;
    private final StringFilterGroup pausedOverlayButtons;
    private final StringFilterGroup metaPanelButton;
    private final ByteArrayFilterGroupList pausedOverlayButtonsGroupList = new ByteArrayFilterGroupList();

    private final StringFilterGroup suggestedAction;
    private final ByteArrayFilterGroupList suggestedActionsGroupList = new ByteArrayFilterGroupList();

    private final StringFilterGroup actionButton;
    private final ByteArrayFilterGroupList videoActionButtonGroupList = new ByteArrayFilterGroupList();
    private final StringFilterGroup shortsActionBar;
    private final StringFilterGroup shortsActionButton;
    private final StringFilterGroupList shortsActionButtonGroupList = new StringFilterGroupList();

    private final ByteArrayFilterGroup useThisSoundButton = new ByteArrayFilterGroup(
            Settings.HIDE_SHORTS_USE_THIS_SOUND_BUTTON,
            "yt_outline_experimental_camera_",
            "yt_outline_camera"
    );

    public ShortsButtonFilter() {
        StringFilterGroup floatingButton = new StringFilterGroup(
                Settings.HIDE_SHORTS_FLOATING_BUTTON,
                "floating_action_button"
        );

        addIdentifierCallbacks(floatingButton);

        pausedOverlayButtons = new StringFilterGroup(
                null,
                "lens_paused_state",
                "shorts_paused_state"
        );

        StringFilterGroup channelBar = new StringFilterGroup(
                Settings.HIDE_SHORTS_CHANNEL_BAR,
                REEL_CHANNEL_BAR_PATH
        );

        StringFilterGroup videoLinkLabel = new StringFilterGroup(
                Settings.HIDE_SHORTS_VIDEO_LINK_LABEL,
                "reel_multi_format_link"
        );

        StringFilterGroup videoTitle = new StringFilterGroup(
                Settings.HIDE_SHORTS_VIDEO_TITLE,
                "shorts_video_title_item"
        );

        StringFilterGroup reelSoundMetadata = new StringFilterGroup(
                Settings.HIDE_SHORTS_SOUND_METADATA_LABEL,
                "reel_sound_metadata"
        );

        StringFilterGroup soundButton = new StringFilterGroup(
                Settings.HIDE_SHORTS_SOUND_BUTTON,
                "reel_pivot_button"
        );

        StringFilterGroup infoPanel = new StringFilterGroup(
                Settings.HIDE_SHORTS_INFO_PANEL,
                "shorts_info_panel_overview"
        );

        StringFilterGroup stickers = new StringFilterGroup(
                Settings.HIDE_SHORTS_STICKERS,
                "stickers_layer.",
                "product_sticker."
        );

        StringFilterGroup liveHeader = new StringFilterGroup(
                Settings.HIDE_SHORTS_LIVE_HEADER,
                "immersive_live_header"
        );

        StringFilterGroup livePreview = new StringFilterGroup(
                Settings.HIDE_SHORTS_LIVE_PREVIEW,
                // Can be removed in the future if a way to disable live Shorts is found.
                "live_preview_page_vm."
        );

        StringFilterGroup previewComment = new StringFilterGroup(
                Settings.HIDE_SHORTS_PREVIEW_COMMENT,
                // Comment box preview that appears on Shorts.
                "participation_bar."
        );

        StringFilterGroup shortsCommentsPanel = new StringFilterGroup(
                Settings.HIDE_SHORTS_COMMENTS_PANEL,
                "participation_composer"
        );

        StringFilterGroup likeButton = new StringFilterGroup(
                Settings.HIDE_SHORTS_LIKE_BUTTON,
                "shorts_like_button.",
                "reel_like_button.",
                "reel_like_toggled_button."
        );

        StringFilterGroup saveButton = new StringFilterGroup(
                Settings.HIDE_SHORTS_SAVE_BUTTON,
                "reel_save_button.e"
        );

        autoDubbedLabel = new StringFilterGroup(
                Settings.HIDE_SHORTS_AUTO_DUBBED_LABEL,
                "badge."
        );

        metaPanelButton = new StringFilterGroup(
                null,
                "|ContainerType|button."
        );

        joinButton = new StringFilterGroup(
                Settings.HIDE_SHORTS_JOIN_BUTTON,
                "sponsor_button"
        );

        subscribeButton = new StringFilterGroup(
                Settings.HIDE_SHORTS_SUBSCRIBE_BUTTON,
                "subscribe_button"
        );

        actionButton = new StringFilterGroup(
                null,
                "reel_action_button.",
                "shorts_video_action_button."
        );

        StringFilterGroup likeFountain = new StringFilterGroup(
                Settings.DISABLE_SHORTS_LIKE_BUTTON_FOUNTAIN_ANIMATION,
                "like_fountain."
        );

        shortsActionBar = new StringFilterGroup(
                null,
                "shorts_action_bar.",
                "reel_action_bar."
        );

        shortsActionButton = new StringFilterGroup(
                null,
                // Can be any of:
                // button.eml
                // shorts_video_action_button.eml
                // reel_action_button.eml
                // reel_pivot_button.eml
                "button.",
                "shorts_video_action_button.",
                "reel_action_button.",
                "reel_pivot_button."
        );

        suggestedAction = new StringFilterGroup(
                null,
                "suggested_action.e",
                "suggested_action_inner."
        );

        if (IS_20_22_OR_GREATER) {
            addPathCallbacks(
                    suggestedAction, joinButton, subscribeButton, metaPanelButton,
                    pausedOverlayButtons, autoDubbedLabel, channelBar, videoLinkLabel,
                    videoTitle, reelSoundMetadata, infoPanel, liveHeader, livePreview,
                    previewComment, soundButton, stickers, likeButton, saveButton, shortsCommentsPanel,
                    likeFountain, shortsActionBar
            );
        } else {
            addPathCallbacks(
                    suggestedAction, actionButton, joinButton, subscribeButton, metaPanelButton,
                    pausedOverlayButtons, autoDubbedLabel, channelBar, videoLinkLabel,
                    videoTitle, reelSoundMetadata, infoPanel, liveHeader, livePreview,
                    previewComment, soundButton, stickers, likeButton, saveButton, shortsCommentsPanel,
                    likeFountain, shortsActionBar
            );
        }

        //
        // Action buttons
        //
        if (!IS_20_22_OR_GREATER) {
            videoActionButtonGroupList.addAll(
                    new ByteArrayFilterGroup(
                            Settings.HIDE_SHORTS_COMMENTS_BUTTON,
                            "ic_right_comment",
                            "reel_comment_button",
                            "youtube_shorts_comment_outline"
                    ),
                    new ByteArrayFilterGroup(
                            Settings.HIDE_SHORTS_SHARE_BUTTON,
                            "ic_right_share",
                            "reel_share_button",
                            "youtube_shorts_share_outline"
                    ),
                    new ByteArrayFilterGroup(
                            Settings.HIDE_SHORTS_REMIX_BUTTON,
                            "ic_remix_filled",
                            "reel_remix_button",
                            "youtube_shorts_remix_outline"
                    ),
                    new ByteArrayFilterGroup(
                            Settings.DISABLE_SHORTS_LIKE_BUTTON_FOUNTAIN_ANIMATION,
                            "shorts_like_fountain"
                    )
            );
        }

        shortsActionButtonGroupList.addAll(
                new StringFilterGroup(
                        Settings.HIDE_SHORTS_COMMENTS_BUTTON,
                        "id.reel_comment_button"
                ),
                new StringFilterGroup(
                        Settings.HIDE_SHORTS_SHARE_BUTTON,
                        "id.reel_share_button"
                ),
                new StringFilterGroup(
                        Settings.HIDE_SHORTS_REMIX_BUTTON,
                        "id.reel_remix_button"
                )
        );

        //
        // Paused overlay buttons.
        //
        pausedOverlayButtonsGroupList.addAll(
                new ByteArrayFilterGroup(
                        Settings.HIDE_SHORTS_TRENDS_BUTTON,
                        "yt_outline_fire_"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_SHORTS_SHOPPING_BUTTON,
                        "yt_outline_bag_"
                )
        );

        //
        // Suggested actions.
        //
        suggestedActionsGroupList.addAll(
                new ByteArrayFilterGroup(
                        Settings.HIDE_SHORTS_TAGGED_PRODUCTS,
                        // Product buttons show pictures of the products, and does not have any unique icons to identify.
                        // Instead, use a unique identifier found in the buffer.
                        "PAproduct_listZ"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_SHORTS_PREVIEW_COMMENT,
                        // Preview comment that can popup while a Short is playing.
                        // Uses no bundled icons, and instead the users profile photo is shown.
                        "shorts-comments-panel"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_SHORTS_SHOP_BUTTON,
                        "yt_outline_experimental_bag_",
                        "yt_outline_bag_"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_SHORTS_LOCATION_BUTTON,
                        "yt_outline_experimental_location_point_",
                        "yt_outline_location_point_"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_SHORTS_SAVE_MUSIC_BUTTON,
                        "yt_outline_experimental_list_add_",
                        "yt_outline_list_add_",
                        "yt_outline_bookmark_"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_SHORTS_SEARCH_SUGGESTIONS_BUTTON,
                        "yt_outline_experimental_search_",
                        "yt_outline_search_"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_SHORTS_BUY_SUPER_THANKS_BUTTON,
                        "yt_outline_experimental_dollar_sign_heart_",
                        "yt_outline_dollar_sign_heart_"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_SHORTS_USE_THIS_TEMPLATE_BUTTON,
                        "yt_outline_experimental_template_add",
                        "yt_outline_template_add"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_SHORTS_GREEN_SCREEN_BUTTON,
                        "greenscreen_temp",
                        "shorts_green_screen"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_SHORTS_HASHTAG_BUTTON,
                        "yt_outline_experimental_hashtag_",
                        "yt_outline_hashtag"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_SHORTS_NEW_POSTS_BUTTON,
                        "yt_outline_experimental_box_pencil",
                        "yt_outline_box_pencil"
                ),
                useThisSoundButton
        );
    }

    private boolean isEverySuggestedActionFilterEnabled() {
        for (ByteArrayFilterGroup group : suggestedActionsGroupList)
            if (!group.isEnabled()) return false;

        return true;
    }

    private boolean isActionBarButtonFiltered(String path, String buttonIdentity) {
        return shortsActionButton.check(path).isFiltered()
                && shortsActionButtonGroupList.check(buttonIdentity).isFiltered();
    }

    @Override
    public boolean isFiltered(String path, String identifier, String allValue, byte[] buffer,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        if (matchedGroup == subscribeButton || matchedGroup == joinButton || matchedGroup == autoDubbedLabel) {
            // Selectively filter to avoid false positive filtering of other subscribe/join buttons.
            return StringUtils.startsWithAny(
                    path,
                    REEL_CHANNEL_BAR_PATH,
                    REEL_LIVE_HEADER_PATH,
                    REEL_METAPANEL_PATH,
                    REEL_PLAYER_OVERLAY_PATH
            );
        }

        if (matchedGroup == metaPanelButton) {
            return StringUtils.startsWithAny(path, REEL_METAPANEL_PATH, REEL_PLAYER_OVERLAY_PATH)
                    && useThisSoundButton.check(buffer).isFiltered();
        }

        // Video action buttons (like, dislike, comment, share, remix) have the same path.
        if (matchedGroup == actionButton) {
            return videoActionButtonGroupList.check(buffer).isFiltered();
        }

        if (matchedGroup == shortsActionBar) {
            return isActionBarButtonFiltered(path, identifier);
        }

        if (matchedGroup == suggestedAction) {
            if (isEverySuggestedActionFilterEnabled()) {
                return true;
            }
            // Suggested actions can be at the start or in the middle of a path.
            return suggestedActionsGroupList.check(buffer).isFiltered();
        }

        if (matchedGroup == pausedOverlayButtons) {
            if (Settings.HIDE_SHORTS_PAUSED_OVERLAY_BUTTONS.get()) {
                return true;
            } else if (StringUtils.contains(path, SHORTS_PAUSED_STATE_BUTTON_PATH)) {
                return pausedOverlayButtonsGroupList.check(buffer).isFiltered();
            }
            return false;
        }

        return true;
    }

    @Override
    public boolean isFiltered(Object contextSource, String identifier, String accessibility, String path, byte[] buffer,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        if (matchedGroup == subscribeButton || matchedGroup == joinButton || matchedGroup == autoDubbedLabel) {
            // Selectively filter to avoid false positive filtering of other subscribe/join buttons.
            return StringUtils.startsWithAny(
                    path,
                    REEL_CHANNEL_BAR_PATH,
                    REEL_LIVE_HEADER_PATH,
                    REEL_METAPANEL_PATH,
                    REEL_PLAYER_OVERLAY_PATH
            );
        }

        if (matchedGroup == metaPanelButton) {
            return StringUtils.startsWithAny(path, REEL_METAPANEL_PATH, REEL_PLAYER_OVERLAY_PATH)
                    && useThisSoundButton.check(buffer).isFiltered();
        }

        if (matchedGroup == shortsActionBar) {
            return isActionBarButtonFiltered(path, accessibility);
        }

        if (matchedGroup == suggestedAction) {
            if (isEverySuggestedActionFilterEnabled()) {
                return true;
            }
            // Suggested actions can be at the start or in the middle of a path.
            return suggestedActionsGroupList.check(buffer).isFiltered();
        }

        if (matchedGroup == pausedOverlayButtons) {
            if (Settings.HIDE_SHORTS_PAUSED_OVERLAY_BUTTONS.get()) {
                return true;
            } else if (StringUtils.contains(path, SHORTS_PAUSED_STATE_BUTTON_PATH)) {
                return pausedOverlayButtonsGroupList.check(buffer).isFiltered();
            }
            return false;
        }

        return true;
    }
}
