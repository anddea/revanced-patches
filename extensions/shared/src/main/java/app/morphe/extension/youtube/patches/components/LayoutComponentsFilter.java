/*
 * Portions of this file are ported from Morphe:
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.extension.youtube.patches.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import app.morphe.extension.shared.patches.components.ByteArrayFilterGroup;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.youtube.patches.utils.ProtoNode;
import android.net.Uri;
import android.view.View;
import app.morphe.extension.shared.patches.components.Filter;
import app.morphe.extension.shared.patches.components.StringFilterGroup;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.shared.NavigationBar.NavigationButton;

@SuppressWarnings("unused")
public final class LayoutComponentsFilter extends Filter {
    private static final String ACCOUNT_HEADER_PATH = "account_header.";
    private static final String HANDLE_PATH = "|CellType|ContainerType|ContainerType|ContainerType|TextType|";

    private final StringFilterGroup channelHandle;
    private final StringFilterGroup postDislikeButton;
    private final StringFilterGroup postCommentsPreview;

    public LayoutComponentsFilter() {
        addIdentifierCallbacks(
                new StringFilterGroup(
                        Settings.HIDE_VISUAL_SPACER,
                        "cell_divider"
                )
        );

        channelHandle = new StringFilterGroup(
                Settings.HIDE_HANDLE,
                "content_metadata.e"
        );

        // The preview of the comments is loaded separately and added to the post, so it's hidden by its component.
        postCommentsPreview = new StringFilterGroup(
                Settings.HIDE_POST_COMMENTS_PREVIEW,
                "post_comments_carousel.e"
        );

        // The like and dislike buttons share their data, so the dislike button alone is hidden by its component.
        postDislikeButton = new StringFilterGroup(
                Settings.HIDE_POST_DISLIKE_BUTTON,
                "post_dislike_button.e"
        );

        addPathCallbacks(
                postCommentsPreview,
                postDislikeButton,
                new StringFilterGroup(
                        Settings.HIDE_HANDLE,
                        ACCOUNT_HEADER_PATH
                ),
                channelHandle
        );
    }

    @Override
    public boolean isFiltered(String path, String identifier, String allValue, byte[] buffer,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        if (matchedGroup == postDislikeButton || matchedGroup == postCommentsPreview) {
            return true;
        }
        if (matchedGroup == channelHandle) {
            return path.startsWith("page_header.e")
                    && NavigationButton.getSelectedNavigationButton() == NavigationButton.LIBRARY;
        }
        return contentType != FilterContentType.PATH || (contentIndex == 0 && path.contains(HANDLE_PATH));
    }

    /**
     * Injection point.
     */
    public static void hideSyncButton(View view) {
        Utils.hideViewBy0dpUnderCondition(Settings.HIDE_SYNC_BUTTON, view);
    }

    /**
     * Hides a search term thumbnail and prevents the image loader from populating it.
     *
     * @param view The thumbnail view supplied to the image loader.
     * @param uri The thumbnail URI supplied to the image loader.
     * @return {@code null} when thumbnails are hidden; otherwise the original URI.
     */
    public static Uri hideSearchTermThumbnails(View view, Uri uri) {
        if (Settings.HIDE_SEARCH_TERM_THUMBNAIL.get()) {
            if (view != null) {
                Utils.hideViewByLayoutParams(view);
            }
            return null;
        }
        return uri;
    }

    /**
     * Layout of the buttons below the posts.
     */
    public enum PostButtonsLayout {
        /**
         * The layout that the server sends.
         */
        SERVER,
        /**
         * The grouped layout of the post template, with the comment and share buttons next to the like and dislike
         * buttons, so every account gets the same buttons and the hidden buttons can be found.
         * The posts of the wide layout (post_base_wrapper_slim) have no grouped layout and keep their layout.
         */
        ENFORCED
    }

    /**
     * Command of the comment button, which opens the comments panel of the post,
     * at the same path below the data of the buttons in every layout of the posts.
     */
    private static final String POST_COMMENTS_COMMAND_PREFIX = "post-comments-";
    private static final int[] POST_COMMENTS_COMMAND_PATH = {3, 1, 169495254, 138681778, 10, 3};

    /**
     * The root templates of the posts, which differ between accounts and screen sizes,
     * and the command of the comment button, which every layout of the posts has.
     */
    private static final ByteArrayFilterGroup postBuffer = new ByteArrayFilterGroup(
            null,
            "_post_root",
            "_post_responsive_root",
            "post_base_wrapper",
            "post_shelf_slim.e",
            POST_COMMENTS_COMMAND_PREFIX
    );

    /**
     * Data of the buttons below a post, in the model of the post: the like and dislike buttons,
     * the share button, the comment button, and the field that turns on the grouped layout.
     * The model is an extension field, which differs between the types of post, such as the images, text,
     * polls and videos.
     */
    private static final int[] POST_BUTTONS_PATH = {1, 1, 5};
    /**
     * Lowest field number of the extension fields, such as the models of the posts.
     */
    private static final int POST_MODEL_MIN_FIELD = 1 << 20;
    private static final int POST_LIKE_DISLIKE_BUTTONS_FIELD = 1;
    private static final int POST_SHARE_BUTTON_FIELD = 2;
    private static final int POST_COMMENT_BUTTON_FIELD = 3;
    private static final int POST_GROUPED_LAYOUT_FIELD = 7;
    /**
     * The grouped layout field set to 1 (field 7, varint).
     */
    private static final byte[] POST_GROUPED_LAYOUT_BYTES = {0x38, 0x01};

    /**
     * Data of the buttons below a post of the wide layout (post_base_wrapper_slim), such as on tablets:
     * the like and dislike buttons, and the fields that show the comment and the share buttons.
     */
    private static final int[] WIDE_POST_BUTTONS_PATH = {263835042, 4, 1, 3};
    private static final int WIDE_POST_LIKE_BUTTON_FIELD = 7;
    private static final int WIDE_POST_DISLIKE_BUTTON_FIELD = 8;
    private static final int WIDE_POST_SHOW_COMMENT_BUTTON_FIELD = 9;
    private static final int WIDE_POST_SHOW_SHARE_BUTTON_FIELD = 10;

    /**
     * Injection point.
     * Removes the data of the hidden buttons below the posts, so the post lays out the other buttons
     * without the space of the hidden ones, and turns on the grouped layout of the buttons.
     * The template of the post leaves out a button without its data in every layout, also in the layouts of other
     * accounts, which the paths of the Litho filters may not match
     * (<a href="https://github.com/MorpheApp/morphe-patches/issues/4367">...</a>).
     */
    public static byte[] setPostButtons(byte[] bytes) {
        try {
            final boolean enforced = Settings.POST_BUTTONS_LAYOUT.get() == PostButtonsLayout.ENFORCED;
            final boolean hideLikeDislike = Settings.HIDE_POST_LIKE_DISLIKE_BUTTONS.get();
            final boolean hideShare = Settings.HIDE_POST_SHARE_BUTTON.get();
            final boolean hideComment = Settings.HIDE_POST_COMMENT_BUTTON.get();
            if ((!enforced && !hideLikeDislike && !hideShare && !hideComment)
                    || !postBuffer.check(bytes).isFiltered()) {
                return bytes;
            }

            List<ProtoNode> element = ProtoNode.parse(bytes);
            if (element == null) {
                return bytes;
            }

            // The data of the buttons is found by the command of the comment button, at any depth, such as in the
            // layouts that wrap another layout of the post. The posts without comments are found by the paths of
            // the known layouts.
            List<ProtoNode> postButtons = new ArrayList<>();
            for (ProtoNode text : ProtoNode.textNodes(element)) {
                if (text.getText().startsWith(POST_COMMENTS_COMMAND_PREFIX)
                        && text.pathEndsWith(POST_COMMENTS_COMMAND_PATH)) {
                    ProtoNode buttons = text;
                    for (int i = 0; i < POST_COMMENTS_COMMAND_PATH.length && buttons != null; i++) {
                        buttons = buttons.getParent();
                    }
                    if (buttons != null && !postButtons.contains(buttons)) {
                        postButtons.add(buttons);
                    }
                }
            }
            for (ProtoNode buttons : ProtoNode.findMessages(element, POST_BUTTONS_PATH)) {
                ProtoNode model = buttons.getParent().getParent().getParent();
                // Only the buttons data of a post model, which has the share or the comment button.
                if (model != null && model.getFieldNumber() >= POST_MODEL_MIN_FIELD && buttons.children != null
                        && (ProtoNode.field(buttons.children, POST_SHARE_BUTTON_FIELD) != null
                        || ProtoNode.field(buttons.children, POST_COMMENT_BUTTON_FIELD) != null)
                        && !postButtons.contains(buttons)) {
                    postButtons.add(buttons);
                }
            }
            for (ProtoNode buttons : ProtoNode.findMessages(element, WIDE_POST_BUTTONS_PATH)) {
                if (!postButtons.contains(buttons)) {
                    postButtons.add(buttons);
                }
            }

            boolean modified = false;
            for (ProtoNode buttons : postButtons) {
                List<ProtoNode> fields = buttons.children;
                if (fields == null) {
                    continue;
                }

                // The wide layout shows the comment and the share buttons with flags.
                ProtoNode showComment = ProtoNode.field(fields, WIDE_POST_SHOW_COMMENT_BUTTON_FIELD);
                ProtoNode showShare = ProtoNode.field(fields, WIDE_POST_SHOW_SHARE_BUTTON_FIELD);
                if (buttons.pathEndsWith(WIDE_POST_BUTTONS_PATH)
                        || (showComment != null && showComment.getVarint() != null)
                        || (showShare != null && showShare.getVarint() != null)) {
                    for (ProtoNode field : new ArrayList<>(fields)) {
                        final int fieldNumber = field.getFieldNumber();
                        if (hideLikeDislike && (fieldNumber == WIDE_POST_LIKE_BUTTON_FIELD
                                || fieldNumber == WIDE_POST_DISLIKE_BUTTON_FIELD)) {
                            field.remove();
                            modified = true;
                        } else if (((hideComment && fieldNumber == WIDE_POST_SHOW_COMMENT_BUTTON_FIELD)
                                || (hideShare && fieldNumber == WIDE_POST_SHOW_SHARE_BUTTON_FIELD))
                                && field.getVarint() != null) {
                            field.setVarint(0);
                            modified = true;
                        }
                    }
                    continue;
                }

                for (ProtoNode field : new ArrayList<>(fields)) {
                    final int fieldNumber = field.getFieldNumber();
                    if ((hideLikeDislike && fieldNumber == POST_LIKE_DISLIKE_BUTTONS_FIELD)
                            || (hideShare && fieldNumber == POST_SHARE_BUTTON_FIELD)
                            || (hideComment && fieldNumber == POST_COMMENT_BUTTON_FIELD)) {
                        field.remove();
                        modified = true;
                    }
                }

                if (enforced && ProtoNode.field(fields, POST_GROUPED_LAYOUT_FIELD) == null) {
                    byte[] data = ProtoNode.write(fields);
                    byte[] grouped = Arrays.copyOf(data, data.length + POST_GROUPED_LAYOUT_BYTES.length);
                    System.arraycopy(POST_GROUPED_LAYOUT_BYTES, 0, grouped, data.length, POST_GROUPED_LAYOUT_BYTES.length);
                    buttons.setPayload(grouped);
                    modified = true;
                }
            }

            return modified ? ProtoNode.write(element) : bytes;
        } catch (Exception ex) {
            Logger.printException(() -> "setPostButtons failure", ex);
        }

        return bytes;
    }
}
