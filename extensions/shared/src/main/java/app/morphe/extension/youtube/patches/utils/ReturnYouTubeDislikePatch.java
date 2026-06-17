/*
 * Copyright (C) 2022-2026 anddea
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

package app.morphe.extension.youtube.patches.utils;

import static app.morphe.extension.shared.returnyoutubedislike.ReturnYouTubeDislike.Vote;
import static app.morphe.extension.youtube.utils.ExtendedUtils.isSpoofingToLessThan;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.ShapeDrawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

import app.morphe.extension.shared.returnyoutubedislike.requests.ReturnYouTubeDislikeApi;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.patches.components.ActionButtonsFilter;
import app.morphe.extension.youtube.patches.components.ActionButtonsFilter.ActionButton;
import app.morphe.extension.youtube.patches.components.ReturnYouTubeDislikeFilterPatch;
import app.morphe.extension.youtube.returnyoutubedislike.ReturnYouTubeDislike;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.shared.BottomSheetState;
import app.morphe.extension.youtube.shared.EngagementPanel;
import app.morphe.extension.youtube.shared.PlayerType;
import app.morphe.extension.youtube.shared.VideoInformation;
import app.morphe.extension.youtube.utils.ThemeUtils;
import kotlin.Unit;

/**
 * Handles all interaction of UI patch components.
 * <p>
 * Known limitation:
 * The implementation of Shorts litho requires blocking the loading the first Short until RYD has completed.
 * This is because it modifies the dislikes text synchronously, and if the RYD fetch has
 * not completed yet then the UI will be temporarily frozen.
 * <p>
 * A (yet to be implemented) solution that fixes this problem.  Any one of:
 * - Modify patch to hook onto the Shorts Litho TextView, and update the dislikes text asynchronously.
 * - Find a way to force Litho to rebuild it's component tree,
 * and use that hook to force the shorts dislikes to update after the fetch is completed.
 * - Hook into the dislikes button image view, and replace the dislikes thumb down image with a
 * generated image of the number of dislikes, then update the image asynchronously.  This Could
 * also be used for the regular video player to give a better UI layout and completely remove
 * the need for the Rolling Number patches.
 */
@SuppressWarnings("unused")
public class ReturnYouTubeDislikePatch {

    public static final boolean IS_SPOOFING_TO_NON_LITHO_SHORTS_PLAYER =
            isSpoofingToLessThan("18.34.00");

    /**
     * RYD data for the current video on screen.
     */
    @Nullable
    private static volatile ReturnYouTubeDislike currentVideoData;

    /**
     * The last litho based Shorts loaded.
     * It may be the same value as {@link #currentVideoData}, but usually is the next short to swipe to.
     */
    @Nullable
    private static volatile ReturnYouTubeDislike lastLithoShortsVideoData;

    /**
     * Because litho Shorts spans are created offscreen after {@link ReturnYouTubeDislikeFilterPatch}More actions
     * detects the video ids, but the current Short can arbitrarily reload the same span,
     * then use the {@link #lastLithoShortsVideoData} if this value is greater than zero.
     */
    @GuardedBy("ReturnYouTubeDislikePatch.class")
    private static int useLithoShortsVideoDataCount;

    /**
     * Last video id prefetched. Field is to prevent prefetching the same video id multiple times in a row.
     */
    @Nullable
    private static volatile String lastPrefetchedVideoId;

    private static final String VIDEO_ACTION_BAR_PREFIX = "video_action_bar.";
    private static final String COMPACTIFY_VIDEO_ACTION_BAR_PREFIX = "compactify_video_action_bar.";
    private static final String ACTION_BUTTON_COUNT_TAG_PREFIX = "revanced_ryd_regular_action_button_count_";
    private static final Spannable ACTION_BUTTON_COUNT_PLACEHOLDER = new SpannableString("");
    private static final int ACTION_BUTTON_COUNT_VERTICAL_SPACING_PIXELS = -Utils.dipToPixels(6);
    private static final int ACTION_BUTTON_COUNT_HORIZONTAL_SPACING_PIXELS = Utils.dipToPixels(4);
    private static final int ACTION_BUTTON_COUNT_RESERVED_HEIGHT_PIXELS = Utils.dipToPixels(18);
    private static final float ACTION_BUTTON_VISUAL_SCALE = 0.85f;
    private static final float ACTION_BUTTON_COUNT_DEFAULT_TEXT_SIZE_SP = 12;
    private static final float ACTION_BUTTON_COUNT_MINIMUM_TEXT_SIZE_SP = 9;
    private static final float ACTION_BUTTON_COUNT_TEXT_SIZE_STEP_SP = 0.5f;

    private static final Map<View, RegularActionButtonViewState> regularActionButtonViewStates =
            new WeakHashMap<>();
    @Nullable
    private static WeakReference<ViewGroup> regularActionButtonCountRoot;
    @Nullable
    private static WeakReference<View> regularLikeActionButton;
    @Nullable
    private static WeakReference<View> regularDislikeActionButton;
    @Nullable
    private static WeakReference<TextView> regularLikeActionButtonCountLabel;
    @Nullable
    private static WeakReference<TextView> regularDislikeActionButtonCountLabel;
    @Nullable
    private static ViewTreeObserver.OnPreDrawListener regularActionButtonCountPositionListener;
    @Nullable
    private static String regularActionButtonCountVideoId;
    @Nullable
    private static CharSequence regularLikeActionButtonCountText;
    @Nullable
    private static CharSequence regularDislikeActionButtonCountText;
    @Nullable
    private static volatile String regularActionButtonCountFetchVideoId;
    @Nullable
    private static WeakReference<ViewGroup> regularActionButtonCountSearchRoot;
    @Nullable
    private static ViewTreeObserver.OnPreDrawListener regularActionButtonCountSearchListener;

    static {
        PlayerType.getOnChange().addObserver((PlayerType type) -> {
            Utils.runOnMainThreadNowOrLater(() -> onPlayerTypeChangedForRegularActionButtonCounts(type));
            return Unit.INSTANCE;
        });
        BottomSheetState.getOnChange().addObserver((BottomSheetState state) -> {
            Utils.runOnMainThreadNowOrLater(() -> onBottomSheetStateChangedForRegularActionButtonCounts(state));
            return Unit.INSTANCE;
        });
        EngagementPanel.getOnChange().addObserver((String panelId) -> {
            Utils.runOnMainThreadNowOrLater(() -> onEngagementPanelChangedForRegularActionButtonCounts(panelId));
            return Unit.INSTANCE;
        });
    }

    public static void onRYDStatusChange() {
        ReturnYouTubeDislikeApi.resetRateLimits();
        // Must remove all values to protect against using stale data
        // if the user enables RYD while a video is on screen.
        clearData();
    }

    private static void clearData() {
        currentVideoData = null;
        lastLithoShortsVideoData = null;
        regularActionButtonCountVideoId = null;
        regularLikeActionButtonCountText = null;
        regularDislikeActionButtonCountText = null;
        regularActionButtonCountFetchVideoId = null;
        Utils.runOnMainThreadNowOrLater(ReturnYouTubeDislikePatch::removeRegularActionButtonCountOverlays);
        synchronized (ReturnYouTubeDislike.class) {
            useLithoShortsVideoDataCount = 0;
        }
        // Rolling number text should not be cleared,
        // as it's used if incognito Short is opened/closed
        // while a regular video is on screen.
    }

    /**
     * @return If {@link #useLithoShortsVideoDataCount} was greater than zero.
     */
    private static boolean decrementUseLithoDataIfNeeded() {
        synchronized (ReturnYouTubeDislikePatch.class) {
            if (useLithoShortsVideoDataCount > 0) {
                useLithoShortsVideoDataCount--;
                return true;
            }

            return false;
        }
    }

    //
    // Litho player for both regular videos and Shorts.
    //

    /**
     * Injection point.
     * <p>
     * For Litho segmented buttons and Litho Shorts player.
     */
    @NonNull
    public static CharSequence onLithoTextLoaded(@NonNull Object conversionContext,
                                                 @NonNull CharSequence original) {
        return onLithoTextLoaded(conversionContext, original, false);
    }

    /**
     * Injection point.
     * <p>
     * Called when a litho text component is initially created,
     * and also when a Span is later reused again (such as scrolling off/on screen).
     * <p>
     * This method is sometimes called on the main thread, but it is usually called _off_ the main thread.
     * This method can be called multiple times for the same UI element (including after dislikes was added).
     *
     * @param original        Original char sequence was created or reused by Litho.
     * @param isRollingNumber If the span is for a Rolling Number.
     * @return The original char sequence (if nothing should change), or a replacement char sequence that contains dislikes.
     */
    @NonNull
    private static CharSequence onLithoTextLoaded(@NonNull Object conversionContext,
                                                  @NonNull CharSequence original,
                                                  boolean isRollingNumber) {
        try {
            if (!Settings.RYD_ENABLED.get()) {
                return original;
            }

            String conversionContextString = conversionContext.toString();

            if (isRollingNumber && !conversionContextString.contains("video_action_bar.")) {
                return original;
            }

            if (conversionContextString.contains("segmented_like_dislike_button.")) {
                // Regular video.
                ReturnYouTubeDislike videoData = currentVideoData;
                if (videoData == null) {
                    return original; // User enabled RYD while a video was on screen.
                }
                if (!(original instanceof Spanned)) {
                    original = new SpannableString(original);
                }
                return videoData.getDislikesSpanForRegularVideo((Spanned) original,
                        true, isRollingNumber);
            }

            if (isRollingNumber) {
                return original; // No need to check for Shorts in the context.
            }

            if (Utils.containsAny(conversionContextString,
                    "|shorts_dislike_button.", "|reel_dislike_button."
            )) {
                return getShortsSpan(original, true);
            }

            if (Utils.containsAny(conversionContextString,
                    "|shorts_like_button.", "|reel_like_button."
            )) {
                if (!Utils.containsNumber(original)) {
                    Logger.printDebug(() -> "Replacing hidden likes count");
                    return getShortsSpan(original, false);
                } else {
                    decrementUseLithoDataIfNeeded();
                }
            }
        } catch (Exception ex) {
            Logger.printException(() -> "onLithoTextLoaded failure", ex);
        }
        return original;
    }

    //
    // Litho Shorts player in the incognito mode / live stream.
    //

    /**
     * Injection point.
     * <p>
     * This method is used in the following situations.
     * <p>
     * 1. When the dislike counts are fetched in the Incognito mode.
     * 2. When the dislike counts are fetched in the live stream.
     *
     * @param original Original span that was created or reused by Litho.
     * @return The original span (if nothing should change), or a replacement span that contains dislikes.
     */
    public static CharSequence onCharSequenceLoaded(@NonNull Object conversionContext,
                                                    @NonNull CharSequence original) {
        try {
            String conversionContextString = conversionContext.toString();
            if (!Settings.RYD_ENABLED.get()) {
                return original;
            }
            if (!Settings.RYD_SHORTS.get()) {
                return original;
            }

            final boolean fetchDislikeLiveStream =
                    conversionContextString.contains("immersive_live_video_action_bar.")
                            && conversionContextString.contains("|dislike_button.");

            if (!fetchDislikeLiveStream) {
                return original;
            }

            ReturnYouTubeDislike videoData = ReturnYouTubeDislike.getFetchForVideoId(ReturnYouTubeDislikeFilterPatch.getShortsVideoId());
            videoData.setVideoIdIsShort(true);
            lastLithoShortsVideoData = videoData;
            synchronized (ReturnYouTubeDislikePatch.class) {
                // Use litho Shorts data for the next like and dislike spans.
                useLithoShortsVideoDataCount = 2;
            }

            return videoData.getDislikeSpanForShort(SHORTS_LOADING_SPAN);
        } catch (Exception ex) {
            Logger.printException(() -> "onCharSequenceLoaded failure", ex);
        }
        return original;
    }


    private static CharSequence getShortsSpan(@NonNull CharSequence original, boolean isDislikesSpan) {
        // Litho Shorts player.
        if (!Settings.RYD_SHORTS.get() || (isDislikesSpan && Settings.HIDE_SHORTS_DISLIKE_BUTTON.get())
                || (!isDislikesSpan && Settings.HIDE_SHORTS_LIKE_BUTTON.get())) {
            return original;
        }

        final ReturnYouTubeDislike videoData;
        if (decrementUseLithoDataIfNeeded()) {
            // New Short is loading off-screen.
            videoData = lastLithoShortsVideoData;
        } else {
            videoData = currentVideoData;
        }

        if (videoData == null) {
            // The Shorts litho video id filter did not detect the video id.
            // This is normal in incognito mode, but otherwise is abnormal.
            Logger.printDebug(() -> "Cannot modify Shorts litho span, data is null");
            return original;
        }

        return isDislikesSpan
                ? videoData.getDislikeSpanForShort((Spanned) original)
                : videoData.getLikeSpanForShort((Spanned) original);
    }

    //
    // Rolling Number
    //

    /**
     * Current regular video rolling number text, if rolling number is in use.
     * This is saved to a field as it's used in every draw() call.
     */
    @Nullable
    private static volatile CharSequence rollingNumberSpan;

    /**
     * Injection point.
     */
    public static String onRollingNumberLoaded(@NonNull Object conversionContext,
                                               @NonNull String original) {
        try {
            CharSequence replacement = onLithoTextLoaded(conversionContext, original, true);

            String replacementString = replacement.toString();
            if (!replacementString.equals(original)) {
                rollingNumberSpan = replacement;
                return replacementString;
            } // Else, the text was not a likes count but instead the view count or something else.
        } catch (Exception ex) {
            Logger.printException(() -> "onRollingNumberLoaded failure", ex);
        }
        return original;
    }

    /**
     * Injection point.
     * <p>
     * Called for all usage of Rolling Number.
     * Modifies the measured String text width to include the left separator and padding, if needed.
     */
    public static float onRollingNumberMeasured(String text, float measuredTextWidth) {
        try {
            if (Settings.RYD_ENABLED.get()) {
                if (ReturnYouTubeDislike.isPreviouslyCreatedSegmentedSpan(text)) {
                    // +1 pixel is needed for some foreign languages that measure
                    // the text different from what is used for layout (Greek in particular).
                    // Probably a bug in Android, but who knows.
                    // Single line mode is also used as an additional fix for this issue.
                    if (Settings.RYD_COMPACT_LAYOUT.get()) {
                        return measuredTextWidth + 1;
                    }

                    return measuredTextWidth + 1
                            + ReturnYouTubeDislike.leftSeparatorBounds.right
                            + ReturnYouTubeDislike.leftSeparatorShapePaddingPixels;
                }
            }
        } catch (Exception ex) {
            Logger.printException(() -> "onRollingNumberMeasured failure", ex);
        }

        return measuredTextWidth;
    }

    /**
     * Add Rolling Number text view modifications.
     */
    private static void addRollingNumberPatchChanges(TextView view) {
        // YouTube Rolling Numbers do not use compound drawables or drawable padding.
        if (view.getCompoundDrawablePadding() == 0) {
            Logger.printDebug(() -> "Adding rolling number TextView changes");
            view.setCompoundDrawablePadding(ReturnYouTubeDislike.leftSeparatorShapePaddingPixels);
            ShapeDrawable separator = ReturnYouTubeDislike.getLeftSeparatorDrawable();

            if (Utils.isRightToLeftLocale()) {
                view.setCompoundDrawables(null, null, separator, null);
            } else {
                view.setCompoundDrawables(separator, null, null, null);
            }

            // Disliking can cause the span to grow in size, which is ok and is laid out correctly,
            // but if the user then removes their dislike the layout will not adjust to the new shorter width.
            // Use a center alignment to take up any extra space.
            view.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

            // Single line mode does not clip words if the span is larger than the view bounds.
            // The styled span applied to the view should always have the same bounds,
            // but use this feature just in case the measurements are somehow off by a few pixels.
            view.setSingleLine(true);
        }
    }

    /**
     * Remove Rolling Number text view modifications made by this patch.
     * Required as it appears text views can be reused for other rolling numbers (view count, upload time, etc.).
     */
    private static void removeRollingNumberPatchChanges(TextView view) {
        if (view.getCompoundDrawablePadding() != 0) {
            Logger.printDebug(() -> "Removing rolling number TextView changes");
            view.setCompoundDrawablePadding(0);
            view.setCompoundDrawables(null, null, null, null);
            view.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY); // Default alignment
            view.setSingleLine(false);
        }
    }

    /**
     * Injection point.
     */
    public static CharSequence updateRollingNumber(TextView view, CharSequence original) {
        try {
            if (!Settings.RYD_ENABLED.get()) {
                removeRollingNumberPatchChanges(view);
                return original;
            }
            final boolean isDescriptionPanel = view.getParent() instanceof ViewGroup viewGroupParent &&
                    viewGroupParent.getChildCount() < 2;
            // Called for all instances of RollingNumber, so must check if text is for a dislikes.
            // Text will already have the correct content, but it's missing the drawable separators.
            if (!ReturnYouTubeDislike.isPreviouslyCreatedSegmentedSpan(original.toString()) || isDescriptionPanel) {
                // The text is the video view count, upload time, or some other text.
                removeRollingNumberPatchChanges(view);
                return original;
            }

            CharSequence replacement = rollingNumberSpan;
            if (replacement == null) {
                // User enabled RYD while a video was open,
                // or user opened/closed a Short while a regular video was opened.
                Logger.printDebug(() -> "Cannot update rolling number (field is null)");
                removeRollingNumberPatchChanges(view);
                return original;
            }

            if (Settings.RYD_COMPACT_LAYOUT.get()) {
                removeRollingNumberPatchChanges(view);
            } else {
                addRollingNumberPatchChanges(view);
            }

            // Remove any padding set by Rolling Number.
            view.setPadding(0, 0, 0, 0);

            // When displaying dislikes, the rolling animation is not visually correct
            // and the dislikes always animate (even though the dislike count has not changed).
            // The animation is caused by an image span attached to the span,
            // and using only the modified segmented span prevents the animation from showing.
            return replacement;
        } catch (Exception ex) {
            Logger.printException(() -> "updateRollingNumber failure", ex);
            return original;
        }
    }

    /**
     * Injection point.
     * <p>
     * Called when a regular video EML action bar has lazily converted its action buttons.
     */
    public static void onLazilyConvertedElementLoaded(@NonNull List<Object> treeNodeResultList,
                                                      @NonNull String identifier) {
        if (!Settings.RYD_ENABLED.get() || !isRegularVideoActionBar(identifier)) {
            return;
        }

        scheduleRegularActionButtonCountOverlayUpdates();
    }

    private static boolean isRegularVideoActionBar(@NonNull String identifier) {
        return identifier.startsWith(VIDEO_ACTION_BAR_PREFIX)
                || identifier.startsWith(COMPACTIFY_VIDEO_ACTION_BAR_PREFIX);
    }

    private static void onPlayerTypeChangedForRegularActionButtonCounts(@NonNull PlayerType playerType) {
        if (!Settings.RYD_ENABLED.get()) {
            removeRegularActionButtonCountOverlays();
            return;
        }

        if (!canShowRegularActionButtonCountOverlays(playerType)) {
            removeRegularActionButtonCountOverlays();
            return;
        }

        if (currentVideoData != null) {
            scheduleRegularActionButtonCountOverlayUpdates();
        }
    }

    private static void onBottomSheetStateChangedForRegularActionButtonCounts(@NonNull BottomSheetState state) {
        if (state.isOpen() || !Settings.RYD_ENABLED.get()) {
            removeRegularActionButtonCountOverlays();
            return;
        }

        if (currentVideoData != null) {
            scheduleRegularActionButtonCountOverlayUpdates();
        }
    }

    private static void onEngagementPanelChangedForRegularActionButtonCounts(@NonNull String panelId) {
        if (!panelId.isEmpty() || !Settings.RYD_ENABLED.get()) {
            removeRegularActionButtonCountOverlays();
            return;
        }

        if (currentVideoData != null) {
            scheduleRegularActionButtonCountOverlayUpdates();
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean canShowRegularActionButtonCountOverlays() {
        return canShowRegularActionButtonCountOverlays(PlayerType.getCurrent());
    }

    private static boolean canShowRegularActionButtonCountOverlays(@NonNull PlayerType playerType) {
        if (Settings.DISABLE_LAYOUT_UPDATES.get() || Settings.RESTORE_OLD_VIDEO_ACTION_BAR.get()) {
            return false;
        }

        return playerType == PlayerType.WATCH_WHILE_MAXIMIZED
                && !BottomSheetState.getCurrent().isOpen()
                && !EngagementPanel.isOpen()
                // Segmented/old layouts already receive the count through onLithoTextLoaded().
                && !ActionButtonsFilter.hasCurrentVideoActionButton(ActionButton.LIKE_DISLIKE);
    }

    /**
     * Schedules updates to the regular video action bar overlays (likes and dislikes count).
     * <p>
     * Before starting the asynchronous background fetch task for RYD data, we attempt to find
     * the action button anchors on the main thread and parse the original like count from the
     * accessibility content description. This immediately populates the cached original like count,
     * allowing the background thread to fetch estimated and original values without blocking/sleeping.
     */
    private static void scheduleRegularActionButtonCountOverlayUpdates() {
        ReturnYouTubeDislike videoData = currentVideoData;
        if (videoData == null) {
            removeRegularActionButtonCountOverlays();
            return;
        }

        ViewGroup decorViewRoot = getDecorViewRoot();
        if (decorViewRoot != null) {
            RegularActionButtonAnchors anchors = findRegularActionButtonAnchors(decorViewRoot);
            if (anchors != null && anchors.likeButton() != null) {
                if (isLikeButtonLiked(anchors.likeButton())) {
                    videoData.setUserVote(Vote.LIKE);
                } else if (anchors.dislikeButton() != null && isDislikeButtonDisliked(anchors.dislikeButton())) {
                    videoData.setUserVote(Vote.DISLIKE);
                } else {
                    if (videoData.getUserVote() == null) {
                        videoData.setUserVote(Vote.LIKE_REMOVE);
                    }
                }
                CharSequence contentDesc = anchors.likeButton().getContentDescription();
                parseAndSetOriginalLikeCount(contentDesc);
            }
        }

        updateRegularActionButtonCountOverlaysFromCache(videoData);
        if (!canShowRegularActionButtonCountOverlays()) {
            hideTrackedRegularActionButtonCountLabels();
        }

        String videoId = videoData.getVideoId();
        if (videoId.equals(regularActionButtonCountVideoId)
                && regularLikeActionButtonCountText != null
                && regularDislikeActionButtonCountText != null) {
            return;
        }

        synchronized (ReturnYouTubeDislikePatch.class) {
            if (videoId.equals(regularActionButtonCountFetchVideoId)) {
                return;
            }
            regularActionButtonCountFetchVideoId = videoId;
        }

        updateRegularActionButtonCountOverlaysAsync(videoData);
    }

    private static void updateRegularActionButtonCountOverlaysAsync(@NonNull ReturnYouTubeDislike videoData) {
        Utils.runOnBackgroundThread(() -> {
            Spanned likeCount = videoData.getLikeSpanForRegularVideoActionButton(ACTION_BUTTON_COUNT_PLACEHOLDER);
            Spanned dislikeCount = videoData.getDislikeSpanForRegularVideoActionButton(ACTION_BUTTON_COUNT_PLACEHOLDER);

            synchronized (ReturnYouTubeDislikePatch.class) {
                if (videoData.getVideoId().equals(regularActionButtonCountFetchVideoId)) {
                    regularActionButtonCountFetchVideoId = null;
                }
            }

            if (likeCount.length() == 0 && dislikeCount.length() == 0) {
                return;
            }

            Utils.runOnMainThreadNowOrLater(() ->
                    updateRegularActionButtonCountOverlays(videoData.getVideoId(), likeCount, dislikeCount));
        });
    }

    private static void updateRegularActionButtonCountOverlaysFromCache(@NonNull ReturnYouTubeDislike videoData) {
        if (!videoData.getVideoId().equals(regularActionButtonCountVideoId)
                || regularLikeActionButtonCountText == null
                || regularDislikeActionButtonCountText == null) {
            return;
        }

        updateRegularActionButtonCountOverlays(
                videoData.getVideoId(),
                regularLikeActionButtonCountText,
                regularDislikeActionButtonCountText
        );
    }

    /**
     * Updates the regular action button count overlays on the screen.
     * <p>
     * Parses the like count from the like button's content description. If it differs from the
     * previously cached value (or is parsed for the first time), it clears the layout caches and
     * schedules another layout update to refresh the button overlays with the exact count.
     *
     * @param videoId      The current video ID.
     * @param likeCount    The formatted like count CharSequence.
     * @param dislikeCount The formatted dislike count CharSequence.
     */
    private static void updateRegularActionButtonCountOverlays(@NonNull String videoId,
                                                               @NonNull CharSequence likeCount,
                                                               @NonNull CharSequence dislikeCount) {
        try {
            ReturnYouTubeDislike currentData = currentVideoData;
            if (currentData == null || !videoId.equals(currentData.getVideoId())) {
                removeRegularActionButtonCountOverlays();
                return;
            }

            regularActionButtonCountVideoId = videoId;
            regularLikeActionButtonCountText = likeCount;
            regularDislikeActionButtonCountText = dislikeCount;

            ViewGroup decorViewRoot = getDecorViewRoot();
            if (decorViewRoot == null) {
                return;
            }

            if (!canShowRegularActionButtonCountOverlays()) {
                // Player, bottom-sheet, and engagement-panel transitions reschedule explicitly.
                // Do not keep a per-frame search alive while another surface owns the action row.
                removeRegularActionButtonCountSearchUpdates();
                hideTrackedRegularActionButtonCountLabels();
                return;
            }

            RegularActionButtonAnchors anchors = findRegularActionButtonAnchors(decorViewRoot);
            if (anchors == null) {
                if (hasVisibleSegmentedRegularActionButtonCount(decorViewRoot)) {
                    removeRegularActionButtonCountSearchUpdates();
                    hideTrackedRegularActionButtonCountLabels();
                    return;
                }
                ensureRegularActionButtonCountSearchUpdates(decorViewRoot);
                hideTrackedRegularActionButtonCountLabels();
                return;
            }

            if (anchors.likeButton() != null) {
                if (isLikeButtonLiked(anchors.likeButton())) {
                    currentData.setUserVote(Vote.LIKE);
                } else if (anchors.dislikeButton() != null && isDislikeButtonDisliked(anchors.dislikeButton())) {
                    currentData.setUserVote(Vote.DISLIKE);
                } else {
                    if (currentData.getUserVote() == null) {
                        currentData.setUserVote(Vote.LIKE_REMOVE);
                    }
                }
                CharSequence contentDesc = anchors.likeButton().getContentDescription();
                Long oldLikes = VideoInformation.getOriginalLikeCount();
                parseAndSetOriginalLikeCount(contentDesc);
                Long newLikes = VideoInformation.getOriginalLikeCount();
                if (!Objects.equals(oldLikes, newLikes)) {
                    regularLikeActionButtonCountText = null;
                    regularDislikeActionButtonCountText = null;
                    synchronized (ReturnYouTubeDislikePatch.class) {
                        regularActionButtonCountFetchVideoId = null;
                    }
                    Utils.runOnMainThreadNowOrLater(ReturnYouTubeDislikePatch::scheduleRegularActionButtonCountOverlayUpdates);
                }
            }

            ViewGroup overlayHost = findSuitableOverlayHost(anchors.getVisibleButton(), decorViewRoot);

            ViewGroup previousRoot = regularActionButtonCountRoot == null
                    ? null
                    : regularActionButtonCountRoot.get();
            if (previousRoot != null && previousRoot != overlayHost) {
                removeTrackedRegularActionButtonCountLabels();
                removeRegularActionButtonCountPositionUpdates();
                restoreRegularActionButtons();
            }

            removeRegularActionButtonCountSearchUpdates();
            TextView likeLabel = null;
            TextView dislikeLabel = null;
            if (anchors.likeButton() != null) {
                likeLabel = getOrCreateRegularActionButtonCountLabel(overlayHost, true);
            } else {
                removeRegularActionButtonCountLabel(true);
            }
            if (anchors.dislikeButton() != null) {
                dislikeLabel = getOrCreateRegularActionButtonCountLabel(overlayHost, false);
            } else {
                removeRegularActionButtonCountLabel(false);
            }
            setRegularActionButtonCountAnchors(
                    overlayHost,
                    anchors.likeButton(),
                    anchors.dislikeButton(),
                    likeLabel,
                    dislikeLabel
            );
            positionRegularActionButtonCountLabels(
                    overlayHost,
                    anchors.likeButton(),
                    likeLabel,
                    likeCount,
                    anchors.dislikeButton(),
                    dislikeLabel,
                    dislikeCount
            );
        } catch (Exception ex) {
            Logger.printException(() -> "updateRegularActionButtonCountOverlays failure", ex);
        }
    }

    @Nullable
    private static RegularActionButtonAnchors findRegularActionButtonAnchors(@NonNull ViewGroup root) {
        int likeButtonId = Utils.getResourceIdentifier("like_button", "id");
        int dislikeButtonId = Utils.getResourceIdentifier("dislike_button", "id");
        List<View> likeButtons = new ArrayList<>();
        List<View> dislikeButtons = new ArrayList<>();

        collectRegularActionButtonCandidates(root, likeButtonId, true, likeButtons);
        collectRegularActionButtonCandidates(root, dislikeButtonId, false, dislikeButtons);

        View[] buttons = findBestRegularActionButtonPair(likeButtons, dislikeButtons);
        if (buttons != null) {
            if (hasExistingSegmentedActionButtonCount(root, buttons[0], buttons[1])) {
                return null;
            }

            ViewGroup actionButtonHost = findNearestCommonParent(buttons[0], buttons[1]);
            if (actionButtonHost == null || actionButtonHost == root) {
                return null;
            }

            resizeRegularActionButtonRow(actionButtonHost, buttons[0], buttons[1]);
            return new RegularActionButtonAnchors(buttons[0], buttons[1]);
        }

        // Keep each overlay independent so hiding one button does not suppress the other count.
        if (!likeButtons.isEmpty() && !dislikeButtons.isEmpty()) {
            return null;
        }

        View likeButton = findBestRegularActionButtonCandidate(likeButtons, regularLikeActionButton);
        View dislikeButton = findBestRegularActionButtonCandidate(dislikeButtons, regularDislikeActionButton);
        // Separate EML buttons can omit a useful Like content description (for example, when
        // YouTube reports the count as "N/A"). The watch-next model still has stable Like and
        // Dislike positions, so resolve the missing native view relative to the known anchor.
        if (likeButton == null && dislikeButton != null) {
            likeButton = findRegularActionButtonByOrder(root, dislikeButton, ActionButton.DISLIKE, ActionButton.LIKE);
        } else if (dislikeButton == null && likeButton != null) {
            dislikeButton = findRegularActionButtonByOrder(root, likeButton, ActionButton.LIKE, ActionButton.DISLIKE);
        }
        View anchorButton = likeButton != null ? likeButton : dislikeButton;
        if (anchorButton == null) {
            return null;
        }

        ViewGroup actionButtonHost = findRegularActionButtonRowHost(root, anchorButton);
        if (actionButtonHost != null) {
            resizeRegularActionButtonRow(actionButtonHost, anchorButton);
        } else {
            resizeRegularActionButton(anchorButton);
        }
        return new RegularActionButtonAnchors(likeButton, dislikeButton);
    }

    /**
     * Resolves an EML action button from the locale-independent order parsed by
     * {@link ActionButtonsFilter}. The known native anchor provides the row and index offset,
     * avoiding a decor-tree guess when the target button has no usable accessibility text.
     */
    @Nullable
    private static View findRegularActionButtonByOrder(@NonNull ViewGroup root,
                                                       @NonNull View knownAnchor,
                                                       @NonNull ActionButton knownAction,
                                                       @NonNull ActionButton targetAction) {
        int knownActionIndex = ActionButtonsFilter.getCurrentVideoActionButtonIndex(knownAction);
        int targetActionIndex = ActionButtonsFilter.getCurrentVideoActionButtonIndex(targetAction);
        if (knownActionIndex < 0 || targetActionIndex < 0) {
            return null;
        }

        ViewGroup rowHost = findRegularActionButtonRowHost(root, knownAnchor);
        if (rowHost == null) {
            return null;
        }

        int rowCenterY = getViewWindowBounds(knownAnchor).centerY();
        int rowTolerance = Utils.dipToPixels(18);
        List<View> rowButtons = new ArrayList<>();
        collectRegularActionButtonRowCandidates(rowHost, rowButtons);
        rowButtons.removeIf(view -> Math.abs(getViewWindowBounds(view).centerY() - rowCenterY) > rowTolerance);
        rowButtons.sort((first, second) -> {
            int comparison = Integer.compare(
                    getViewWindowBounds(first).centerX(),
                    getViewWindowBounds(second).centerX()
            );
            return rowHost.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL ? -comparison : comparison;
        });

        int knownViewIndex = rowButtons.indexOf(knownAnchor);
        if (knownViewIndex < 0) {
            return null;
        }

        int targetViewIndex = knownViewIndex + targetActionIndex - knownActionIndex;
        return targetViewIndex >= 0 && targetViewIndex < rowButtons.size()
                ? rowButtons.get(targetViewIndex)
                : null;
    }

    @NonNull
    private static ViewGroup findSuitableOverlayHost(@NonNull View anchor, @NonNull ViewGroup fallbackRoot) {
        ViewGroup current = anchor.getParent() instanceof ViewGroup ? (ViewGroup) anchor.getParent() : null;
        while (current != null && current != fallbackRoot) {
            String name = current.getClass().getName();
            // We want a standard ViewGroup that can host our overlays properly without overriding absolute positioning.
            if (!name.contains("ComponentHost") && !name.contains("LithoView") && !name.contains("RecyclerView") && !name.contains("LinearLayout")) {
                if (current instanceof android.widget.FrameLayout || current instanceof android.widget.RelativeLayout) {
                    return current;
                }
            }
            current = current.getParent() instanceof ViewGroup ? (ViewGroup) current.getParent() : null;
        }
        return fallbackRoot;
    }

    @Nullable
    private static View[] findRegularActionButtonPair(@NonNull ViewGroup root) {
        int likeButtonId = Utils.getResourceIdentifier("like_button", "id");
        int dislikeButtonId = Utils.getResourceIdentifier("dislike_button", "id");
        List<View> likeButtons = new ArrayList<>();
        List<View> dislikeButtons = new ArrayList<>();

        collectRegularActionButtonCandidates(root, likeButtonId, true, likeButtons);
        collectRegularActionButtonCandidates(root, dislikeButtonId, false, dislikeButtons);

        return findBestRegularActionButtonPair(likeButtons, dislikeButtons);
    }

    @Nullable
    private static View[] findBestRegularActionButtonPair(@NonNull List<View> likeButtons,
                                                          @NonNull List<View> dislikeButtons) {
        View bestLikeButton = null;
        View bestDislikeButton = null;
        int bestScore = Integer.MAX_VALUE;
        int maximumVerticalDistance = Utils.dipToPixels(48);

        for (View likeButton : likeButtons) {
            Rect likeBounds = getViewWindowBounds(likeButton);
            int likeCenterX = likeBounds.centerX();
            int likeCenterY = likeBounds.centerY();

            for (View dislikeButton : dislikeButtons) {
                if (likeButton == dislikeButton) {
                    continue;
                }

                Rect dislikeBounds = getViewWindowBounds(dislikeButton);
                int dislikeCenterX = dislikeBounds.centerX();
                int dislikeCenterY = dislikeBounds.centerY();
                if (likeCenterX >= dislikeCenterX) {
                    continue;
                }

                int verticalDistance = Math.abs(likeCenterY - dislikeCenterY);
                if (verticalDistance > maximumVerticalDistance) {
                    continue;
                }

                // Prefer the topmost visible like/dislike pair. This avoids matching lower feed/comment buttons.
                int score = Math.min(likeBounds.top, dislikeBounds.top) + verticalDistance;
                if (score < bestScore) {
                    bestScore = score;
                    bestLikeButton = likeButton;
                    bestDislikeButton = dislikeButton;
                }
            }
        }

        return bestLikeButton == null ? null : new View[]{bestLikeButton, bestDislikeButton};
    }

    @Nullable
    private static View findBestRegularActionButtonCandidate(@NonNull List<View> candidates,
                                                             @Nullable WeakReference<View> trackedReference) {
        View trackedButton = trackedReference == null ? null : trackedReference.get();
        if (trackedButton != null && candidates.contains(trackedButton)) {
            return trackedButton;
        }
        if (candidates.isEmpty()) {
            return null;
        }
        if (candidates.size() == 1) {
            return candidates.get(0);
        }

        View bestCandidate = null;
        int bestScore = Integer.MIN_VALUE;
        for (View candidate : candidates) {
            Rect bounds = getViewWindowBounds(candidate);
            int score = bounds.centerY();
            if (candidate.getParent() instanceof ViewGroup parent) {
                score += Math.min(parent.getChildCount(), 5) * 1_000;
            }
            if (bestCandidate == null || score > bestScore) {
                bestCandidate = candidate;
                bestScore = score;
            }
        }
        return bestCandidate;
    }

    private static void collectRegularActionButtonCandidates(@NonNull View view,
                                                             int resourceId,
                                                             boolean likeButton,
                                                             @NonNull List<View> candidates) {
        if (!isVisibleOnScreen(view) || isRegularActionButtonCountOverlay(view)) {
            return;
        }

        boolean idMatches = resourceId != 0 && view.getId() == resourceId;
        boolean contentDescriptionMatches = hasRegularActionButtonContentDescription(view, likeButton);
        if ((idMatches || contentDescriptionMatches) && !isCommentActionButtonCandidate(view)) {
            candidates.add(view);
        }

        if (view instanceof ViewGroup viewGroup) {
            for (int i = 0, childCount = viewGroup.getChildCount(); i < childCount; i++) {
                collectRegularActionButtonCandidates(viewGroup.getChildAt(i), resourceId, likeButton, candidates);
            }
        }
    }

    private static boolean hasVisibleSegmentedRegularActionButtonCount(@NonNull ViewGroup root) {
        View[] buttons = findRegularActionButtonPair(root);
        return buttons != null && hasExistingSegmentedActionButtonCount(root, buttons[0], buttons[1]);
    }

    private static boolean contentDescriptionStringsCached = false;
    private static String cachedLikeStr = null;
    private static String cachedUndoLikeStr = null;
    private static String cachedDislikeStr = null;
    private static String cachedUndoDislikeStr = null;

    @SuppressLint("DiscouragedApi")
    private static void cacheContentDescriptionStrings() {
        if (contentDescriptionStringsCached) {
            return;
        }
        contentDescriptionStringsCached = true;
        Context context = Utils.getContext();
        if (context != null) {
            Resources resources = context.getResources();
            String packageName = context.getPackageName();

            int likeId = resources.getIdentifier("accessibility_like_video", "string", packageName);
            if (likeId != 0) {
                cachedLikeStr = resources.getString(likeId).toLowerCase(Locale.ROOT);
            }

            int undoLikeId = resources.getIdentifier("accessibility_undo_like_video", "string", packageName);
            if (undoLikeId != 0) {
                cachedUndoLikeStr = resources.getString(undoLikeId).toLowerCase(Locale.ROOT);
            }

            int dislikeId = resources.getIdentifier("accessibility_dislike_video", "string", packageName);
            if (dislikeId != 0) {
                cachedDislikeStr = resources.getString(dislikeId).toLowerCase(Locale.ROOT);
            }

            int undoDislikeId = resources.getIdentifier("accessibility_undo_dislike_video", "string", packageName);
            if (undoDislikeId != 0) {
                cachedUndoDislikeStr = resources.getString(undoDislikeId).toLowerCase(Locale.ROOT);
            }
        }
    }

    private static boolean hasRegularActionButtonContentDescription(@NonNull View view, boolean likeButton) {
        CharSequence contentDescription = view.getContentDescription();
        if (contentDescription == null) {
            return false;
        }

        cacheContentDescriptionStrings();

        String description = contentDescription.toString().toLowerCase(Locale.ROOT);

        if (likeButton) {
            boolean noDislike = (cachedDislikeStr == null || !description.contains(cachedDislikeStr)) 
                                && !description.contains("dislike");

            if (cachedLikeStr != null && (description.equals(cachedLikeStr) || (description.contains(cachedLikeStr) && noDislike))) {
                return true;
            }
            if (cachedUndoLikeStr != null && (description.equals(cachedUndoLikeStr) || (description.contains(cachedUndoLikeStr) && noDislike))) {
                return true;
            }
        } else {
            if (cachedDislikeStr != null && (description.equals(cachedDislikeStr) || description.contains(cachedDislikeStr))) {
                return true;
            }
            if (cachedUndoDislikeStr != null && (description.equals(cachedUndoDislikeStr) || description.contains(cachedUndoDislikeStr))) {
                return true;
            }
        }

        return likeButton
                ? description.contains("like") && !description.contains("dislike")
                : description.contains("dislike");
    }

    private static boolean isCommentActionButtonCandidate(@NonNull View view) {
        CharSequence contentDescription = view.getContentDescription();
        if (contentDescription != null
                && contentDescription.toString().toLowerCase(Locale.ROOT).contains("comment")) {
            return true;
        }

        int actionToolbarId = Utils.getResourceIdentifier("action_toolbar", "id");
        if (actionToolbarId == 0) {
            return false;
        }

        ViewParent parent = view.getParent();
        while (parent instanceof View parentView) {
            if (parentView.getId() == actionToolbarId
                    && parentView.getParent() instanceof ViewGroup commentRoot
                    && hasCommentContent(commentRoot)) {
                return true;
            }

            parent = parentView.getParent();
        }

        return false;
    }

    private static boolean hasCommentContent(@NonNull ViewGroup viewGroup) {
        int commentAuthorId = Utils.getResourceIdentifier("comment_author", "id");
        int commentContentId = Utils.getResourceIdentifier("comment_content", "id");
        return (commentAuthorId != 0 && viewGroup.findViewById(commentAuthorId) != null)
                || (commentContentId != 0 && viewGroup.findViewById(commentContentId) != null);
    }

    private static boolean hasExistingSegmentedActionButtonCount(@NonNull ViewGroup root,
                                                                 @NonNull View likeButton,
                                                                 @NonNull View dislikeButton) {
        Rect likeBounds = getViewWindowBounds(likeButton);
        Rect dislikeBounds = getViewWindowBounds(dislikeButton);
        int minimumX = Math.min(likeBounds.centerX(), dislikeBounds.centerX());
        int maximumX = Math.max(likeBounds.centerX(), dislikeBounds.centerX());
        int minimumY = Math.min(likeBounds.top, dislikeBounds.top) - Utils.dipToPixels(12);
        int maximumY = Math.max(likeBounds.bottom, dislikeBounds.bottom) + Utils.dipToPixels(12);

        return containsVisibleNumberTextView(root, minimumX, maximumX, minimumY, maximumY);
    }

    private static boolean containsVisibleNumberTextView(@NonNull View view,
                                                         int minimumX,
                                                         int maximumX,
                                                         int minimumY,
                                                         int maximumY) {
        if (!isVisibleOnScreen(view) || isRegularActionButtonCountOverlay(view)) {
            return false;
        }

        if (view instanceof TextView textView) {
            CharSequence text = textView.getText();
            if (text != null && Utils.containsNumber(text)) {
                Rect bounds = getViewWindowBounds(textView);
                int centerX = bounds.centerX();
                int centerY = bounds.centerY();
                if (centerX > minimumX && centerX < maximumX
                        && centerY > minimumY && centerY < maximumY) {
                    return true;
                }
            }
        }

        if (view instanceof ViewGroup viewGroup) {
            for (int i = 0, childCount = viewGroup.getChildCount(); i < childCount; i++) {
                if (containsVisibleNumberTextView(viewGroup.getChildAt(i),
                        minimumX, maximumX, minimumY, maximumY)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Nullable
    private static ViewGroup findNearestCommonParent(@NonNull View firstView,
                                                     @NonNull View secondView) {
        ViewParent parent = firstView.getParent();
        while (parent instanceof ViewGroup viewGroup) {
            if (isDescendantOf(secondView, viewGroup)) {
                return viewGroup;
            }
            parent = viewGroup.getParent();
        }

        return null;
    }

    /**
     * Finds the nearest ancestor containing the remaining like or dislike button and at least one
     * aligned sibling action button. A single button's immediate EML host contains only that button,
     * while the next shared host contains the complete action row that must be resized consistently.
     */
    @Nullable
    private static ViewGroup findRegularActionButtonRowHost(@NonNull ViewGroup root,
                                                            @NonNull View anchorButton) {
        int rowCenterY = getViewWindowBounds(anchorButton).centerY();
        int rowTolerance = Utils.dipToPixels(18);
        ViewParent parent = anchorButton.getParent();
        while (parent instanceof ViewGroup candidateHost && candidateHost != root) {
            List<View> candidates = new ArrayList<>();
            collectRegularActionButtonRowCandidates(candidateHost, candidates);

            int alignedCandidates = 0;
            for (View candidate : candidates) {
                if (Math.abs(getViewWindowBounds(candidate).centerY() - rowCenterY) <= rowTolerance) {
                    alignedCandidates++;
                    if (alignedCandidates > 1) {
                        return candidateHost;
                    }
                }
            }

            parent = candidateHost.getParent();
        }

        return null;
    }

    private static boolean isDescendantOf(@NonNull View view, @NonNull ViewGroup ancestor) {
        ViewParent parent = view.getParent();
        while (parent instanceof View parentView) {
            if (parentView == ancestor) {
                return true;
            }
            parent = parentView.getParent();
        }

        return false;
    }

    /**
     * Reapplies the complete action-row scale after transient engagement-panel animations.
     * {@link #resizeRegularActionButton(View)} ignores intermediate animation values, so calling
     * this from pre-draw is safe and succeeds as soon as YouTube restores the native scale.
     */
    private static void resizeTrackedRegularActionButtonRow(@NonNull ViewGroup root,
                                                            @Nullable View likeButton,
                                                            @Nullable View dislikeButton) {
        if (Settings.DISABLE_LAYOUT_UPDATES.get()) {
            return;
        }
        View anchorButton = likeButton != null ? likeButton : dislikeButton;
        if (anchorButton == null) {
            return;
        }

        ViewGroup actionButtonHost = likeButton != null && dislikeButton != null
                ? findNearestCommonParent(likeButton, dislikeButton)
                : findRegularActionButtonRowHost(root, anchorButton);

        if (actionButtonHost != null) {
            if (likeButton != null && dislikeButton != null) {
                resizeRegularActionButtonRow(actionButtonHost, likeButton, dislikeButton);
            } else {
                resizeRegularActionButtonRow(actionButtonHost, anchorButton);
            }
        } else {
            resizeRegularActionButton(anchorButton);
        }
    }

    private static void resizeRegularActionButtonRow(@NonNull ViewGroup actionButtonHost,
                                                     @NonNull View... anchorButtons) {
        if (Settings.DISABLE_LAYOUT_UPDATES.get()) {
            return;
        }
        int rowCenterYTotal = 0;
        int anchorCount = 0;
        for (View anchorButton : anchorButtons) {
            if (anchorButton != null) {
                rowCenterYTotal += getViewWindowBounds(anchorButton).centerY();
                anchorCount++;
            }
        }
        if (anchorCount == 0) {
            return;
        }

        int rowCenterY = rowCenterYTotal / anchorCount;
        int rowTolerance = Utils.dipToPixels(18);
        List<View> candidates = new ArrayList<>();
        collectRegularActionButtonRowCandidates(actionButtonHost, candidates);

        int resizedButtons = 0;
        for (View candidate : candidates) {
            Rect bounds = getViewWindowBounds(candidate);
            if (Math.abs(bounds.centerY() - rowCenterY) <= rowTolerance) {
                resizeRegularActionButton(candidate);
                resizedButtons++;
            }
        }

        if (resizedButtons < anchorCount) {
            for (View anchorButton : anchorButtons) {
                if (anchorButton != null) {
                    resizeRegularActionButton(anchorButton);
                }
            }
        }
    }

    private static void collectRegularActionButtonRowCandidates(@NonNull View view,
                                                                @NonNull List<View> candidates) {
        if (!isVisibleOnScreen(view) || isRegularActionButtonCountOverlay(view)) {
            return;
        }

        if (isRegularActionButtonRowCandidate(view)) {
            candidates.add(view);
            return;
        }

        if (view instanceof ViewGroup viewGroup) {
            for (int i = 0, childCount = viewGroup.getChildCount(); i < childCount; i++) {
                collectRegularActionButtonRowCandidates(viewGroup.getChildAt(i), candidates);
            }
        }
    }

    private static boolean isRegularActionButtonRowCandidate(@NonNull View view) {
        int minimumSize = Utils.dipToPixels(28);
        int maximumSize = Utils.dipToPixels(76);
        int width = view.getWidth();
        int height = view.getHeight();
        if (width < minimumSize || height < minimumSize || width > maximumSize || height > maximumSize) {
            return false;
        }

        return view.isClickable() || view.getContentDescription() != null;
    }

    /**
     * Applies the count layout scale after YouTube's transient button animation has settled.
     * Reapplying the scale while that animation is running makes the button alternate between
     * YouTube's animated scale and {@link #ACTION_BUTTON_VISUAL_SCALE} during delayed updates.
     */
    private static void resizeRegularActionButton(@NonNull View button) {
        if (Settings.DISABLE_LAYOUT_UPDATES.get()) {
            return;
        }
        if (button.getWidth() <= 0 || button.getHeight() <= 0) {
            return;
        }

        RegularActionButtonViewState originalState = regularActionButtonViewStates.get(button);
        if (originalState == null) {
            // Action buttons use their default scale after YouTube's initial layout animation.
            if (button.getScaleX() != 1.0f || button.getScaleY() != 1.0f) {
                return;
            }

            originalState = new RegularActionButtonViewState(
                    button.getScaleX(),
                    button.getScaleY(),
                    button.getPivotX(),
                    button.getPivotY()
            );
            regularActionButtonViewStates.put(button, originalState);
        } else if ((button.getScaleX() != ACTION_BUTTON_VISUAL_SCALE
                && button.getScaleX() != originalState.scaleX)
                || (button.getScaleY() != ACTION_BUTTON_VISUAL_SCALE
                && button.getScaleY() != originalState.scaleY)) {
            // Do not fight a selection or replacement animation. A later scheduled update
            // reapplies the count layout after the view returns to its original scale.
            return;
        }

        float pivotX = button.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL ? button.getWidth() : 0;
        if (button.getPivotX() != pivotX) {
            button.setPivotX(pivotX);
        }
        if (button.getPivotY() != 0) {
            button.setPivotY(0);
        }
        if (button.getScaleX() != ACTION_BUTTON_VISUAL_SCALE) {
            button.setScaleX(ACTION_BUTTON_VISUAL_SCALE);
        }
        if (button.getScaleY() != ACTION_BUTTON_VISUAL_SCALE) {
            button.setScaleY(ACTION_BUTTON_VISUAL_SCALE);
        }
    }

    private static void restoreRegularActionButtons() {
        regularActionButtonViewStates.entrySet().removeIf(entry -> {
            View button = entry.getKey();
            if (button == null) {
                return true;
            }

            RegularActionButtonViewState state = entry.getValue();
            button.setScaleX(state.scaleX);
            button.setScaleY(state.scaleY);
            button.setPivotX(state.pivotX);
            button.setPivotY(state.pivotY);
            return true;
        });
    }

    @NonNull
    private static TextView getOrCreateRegularActionButtonCountLabel(@NonNull ViewGroup root,
                                                                     boolean likeButton) {
        WeakReference<TextView> textViewReference = likeButton
                ? regularLikeActionButtonCountLabel
                : regularDislikeActionButtonCountLabel;
        TextView textView = textViewReference == null ? null : textViewReference.get();
        if (textView != null && textView.getParent() == root) {
            return textView;
        }

        if (textView == null) {
            textView = new TextView(root.getContext());
            textView.setTag(getRegularActionButtonCountOverlayTag(likeButton));
            textView.setGravity(Gravity.CENTER);
            textView.setIncludeFontPadding(false);
            textView.setSingleLine(true);
            textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, ACTION_BUTTON_COUNT_DEFAULT_TEXT_SIZE_SP);
            textView.setTextColor(ThemeUtils.getAppForegroundColor());
            textView.setElevation(Utils.dipToPixels(8));

            if (likeButton) {
                regularLikeActionButtonCountLabel = new WeakReference<>(textView);
            } else {
                regularDislikeActionButtonCountLabel = new WeakReference<>(textView);
            }
        } else if (textView.getParent() instanceof ViewGroup previousParent) {
            previousParent.removeView(textView);
        }

        root.addView(textView, createRegularActionButtonCountLayoutParams(root));
        return textView;
    }

    @NonNull
    private static ViewGroup.LayoutParams createRegularActionButtonCountLayoutParams(@NonNull ViewGroup root) {
        if (root instanceof FrameLayout) {
            return new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
            );
        }

        return new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private static void setRegularActionButtonCountAnchors(@NonNull ViewGroup root,
                                                           @Nullable View likeButton,
                                                           @Nullable View dislikeButton,
                                                           @Nullable TextView likeLabel,
                                                           @Nullable TextView dislikeLabel) {
        ViewGroup previousRoot = regularActionButtonCountRoot == null
                ? null
                : regularActionButtonCountRoot.get();
        if (previousRoot != root) {
            removeRegularActionButtonCountPositionUpdates();
        }

        regularActionButtonCountRoot = new WeakReference<>(root);
        regularLikeActionButton = likeButton == null ? null : new WeakReference<>(likeButton);
        regularDislikeActionButton = dislikeButton == null ? null : new WeakReference<>(dislikeButton);
        regularLikeActionButtonCountLabel = likeLabel == null ? null : new WeakReference<>(likeLabel);
        regularDislikeActionButtonCountLabel = dislikeLabel == null ? null : new WeakReference<>(dislikeLabel);

        if (regularActionButtonCountPositionListener == null) {
            regularActionButtonCountPositionListener = () -> {
                updateTrackedRegularActionButtonCountLabelPositions();
                return true;
            };
            root.getViewTreeObserver().addOnPreDrawListener(regularActionButtonCountPositionListener);
        }
    }

    private static void updateTrackedRegularActionButtonCountLabelPositions() {
        ViewGroup root = regularActionButtonCountRoot == null
                ? null
                : regularActionButtonCountRoot.get();
        View likeButton = regularLikeActionButton == null
                ? null
                : regularLikeActionButton.get();
        View dislikeButton = regularDislikeActionButton == null
                ? null
                : regularDislikeActionButton.get();
        TextView likeLabel = regularLikeActionButtonCountLabel == null
                ? null
                : regularLikeActionButtonCountLabel.get();
        TextView dislikeLabel = regularDislikeActionButtonCountLabel == null
                ? null
                : regularDislikeActionButtonCountLabel.get();

        if (root == null || (likeLabel == null && dislikeLabel == null)) {
            removeRegularActionButtonCountPositionUpdates();
            return;
        }

        ReturnYouTubeDislike videoData = currentVideoData;
        if (videoData == null || !canShowRegularActionButtonCountOverlays()) {
            removeRegularActionButtonCountOverlays();
            return;
        }

        if (!trackedRegularActionButtonCountAnchorsAreVisible(root, likeButton, dislikeButton)) {
            RegularActionButtonAnchors anchors = findRegularActionButtonAnchors(root);
            if (anchors == null) {
                if (hasVisibleSegmentedRegularActionButtonCount(root)) {
                    removeRegularActionButtonCountSearchUpdates();
                    hideTrackedRegularActionButtonCountLabels();
                    return;
                }
                ensureRegularActionButtonCountSearchUpdates(root);
                hideTrackedRegularActionButtonCountLabels();
                return;
            }

            likeButton = anchors.likeButton();
            dislikeButton = anchors.dislikeButton();
            regularLikeActionButton = likeButton == null ? null : new WeakReference<>(likeButton);
            regularDislikeActionButton = dislikeButton == null ? null : new WeakReference<>(dislikeButton);
        }

        // Closing an engagement panel with X can expose the action row before YouTube's scale
        // animation finishes. Keep retrying from pre-draw so this path converges to the same
        // resized row as a swipe dismissal without introducing an arbitrary delay.
        resizeTrackedRegularActionButtonRow(root, likeButton, dislikeButton);

        // Always fit from the full cached value. Reusing an ellipsized label would permanently
        // retain truncation after rotation or any other layout expansion.
        CharSequence likeText = Objects.requireNonNullElse(
                regularLikeActionButtonCountText,
                ACTION_BUTTON_COUNT_PLACEHOLDER
        );
        CharSequence dislikeText = Objects.requireNonNullElse(
                regularDislikeActionButtonCountText,
                ACTION_BUTTON_COUNT_PLACEHOLDER
        );
        positionRegularActionButtonCountLabels(
                root,
                likeButton,
                likeLabel,
                likeText,
                dislikeButton,
                dislikeLabel,
                dislikeText
        );
    }

    private static boolean trackedRegularActionButtonCountAnchorsAreVisible(@NonNull ViewGroup root,
                                                                            @Nullable View likeButton,
                                                                            @Nullable View dislikeButton) {
        boolean likeVisible = likeButton == null || (
                likeButton.getWidth() > 0
                        && likeButton.getHeight() > 0
                        && isDescendantOf(likeButton, root)
                        && isVisibleOnScreen(likeButton)
        );
        boolean dislikeVisible = dislikeButton == null || (
                dislikeButton.getWidth() > 0
                        && dislikeButton.getHeight() > 0
                        && isDescendantOf(dislikeButton, root)
                        && isVisibleOnScreen(dislikeButton)
        );
        return (likeButton != null || dislikeButton != null) && likeVisible && dislikeVisible;
    }

    private static void removeRegularActionButtonCountPositionUpdates() {
        ViewGroup root = regularActionButtonCountRoot == null
                ? null
                : regularActionButtonCountRoot.get();
        ViewTreeObserver.OnPreDrawListener listener = regularActionButtonCountPositionListener;
        if (root != null && listener != null) {
            ViewTreeObserver observer = root.getViewTreeObserver();
            if (observer.isAlive()) {
                observer.removeOnPreDrawListener(listener);
            }
        }

        regularActionButtonCountRoot = null;
        regularLikeActionButton = null;
        regularDislikeActionButton = null;
        regularActionButtonCountPositionListener = null;
    }

    private static void ensureRegularActionButtonCountSearchUpdates(@NonNull ViewGroup root) {
        ViewGroup previousRoot = regularActionButtonCountSearchRoot == null
                ? null
                : regularActionButtonCountSearchRoot.get();
        if (previousRoot != null && previousRoot != root) {
            removeRegularActionButtonCountSearchUpdates();
        }
        if (regularActionButtonCountSearchListener != null) {
            return;
        }

        regularActionButtonCountSearchRoot = new WeakReference<>(root);
        regularActionButtonCountSearchListener = () -> {
            ReturnYouTubeDislike videoData = currentVideoData;
            CharSequence likeCount = regularLikeActionButtonCountText;
            CharSequence dislikeCount = regularDislikeActionButtonCountText;
            if (videoData == null || likeCount == null || dislikeCount == null) {
                removeRegularActionButtonCountSearchUpdates();
                return true;
            }

            updateRegularActionButtonCountOverlays(videoData.getVideoId(), likeCount, dislikeCount);
            return true;
        };
        root.getViewTreeObserver().addOnPreDrawListener(regularActionButtonCountSearchListener);
    }

    private static void removeRegularActionButtonCountSearchUpdates() {
        ViewGroup root = regularActionButtonCountSearchRoot == null
                ? null
                : regularActionButtonCountSearchRoot.get();
        ViewTreeObserver.OnPreDrawListener listener = regularActionButtonCountSearchListener;
        if (root != null && listener != null) {
            ViewTreeObserver observer = root.getViewTreeObserver();
            if (observer.isAlive()) {
                observer.removeOnPreDrawListener(listener);
            }
        }

        regularActionButtonCountSearchRoot = null;
        regularActionButtonCountSearchListener = null;
    }

    private static void positionRegularActionButtonCountLabels(@NonNull ViewGroup root,
                                                               @Nullable View likeButton,
                                                               @Nullable TextView likeLabel,
                                                               @NonNull CharSequence likeCount,
                                                               @Nullable View dislikeButton,
                                                               @Nullable TextView dislikeLabel,
                                                               @NonNull CharSequence dislikeCount) {
        Rect likeButtonBounds = likeButton == null ? null : getRegularActionButtonVisualBounds(root, likeButton);
        Rect dislikeButtonBounds = dislikeButton == null ? null : getRegularActionButtonVisualBounds(root, dislikeButton);
        int countTop = getRegularActionButtonCountTop(likeButtonBounds, dislikeButtonBounds);

        int rootWidth = Math.max(1, root.getWidth());
        int likeAvailableWidth = likeButtonBounds == null
                ? 0
                : getRegularActionButtonCountAvailableWidth(
                        rootWidth,
                        likeButtonBounds,
                        dislikeButtonBounds
                );

        int dislikeAvailableWidth = dislikeButtonBounds == null
                ? 0
                : getRegularActionButtonCountAvailableWidth(
                        rootWidth,
                        dislikeButtonBounds,
                        likeButtonBounds
                );

        float sharedTextSizeSp = getRegularActionButtonCountTextSizeSp(
                likeLabel,
                likeCount,
                likeAvailableWidth,
                dislikeLabel,
                dislikeCount,
                dislikeAvailableWidth
        );

        if (likeButtonBounds != null && likeLabel != null) {
            positionRegularActionButtonCountLabel(
                    root,
                    likeButtonBounds,
                    likeLabel,
                    likeCount,
                    countTop,
                    likeAvailableWidth,
                    sharedTextSizeSp
            );
        } else if (likeLabel != null) {
            likeLabel.setVisibility(View.GONE);
        }
        if (dislikeButtonBounds != null && dislikeLabel != null) {
            positionRegularActionButtonCountLabel(
                    root,
                    dislikeButtonBounds,
                    dislikeLabel,
                    dislikeCount,
                    countTop,
                    dislikeAvailableWidth,
                    sharedTextSizeSp
            );
        } else if (dislikeLabel != null) {
            dislikeLabel.setVisibility(View.GONE);
        }
    }

    private static int getRegularActionButtonCountTop(@Nullable Rect likeButtonBounds,
                                                      @Nullable Rect dislikeButtonBounds) {
        if (likeButtonBounds == null && dislikeButtonBounds == null) {
            return 0;
        }
        if (likeButtonBounds == null) {
            return Objects.requireNonNull(dislikeButtonBounds).bottom
                    + ACTION_BUTTON_COUNT_VERTICAL_SPACING_PIXELS;
        }
        if (dislikeButtonBounds == null) {
            return likeButtonBounds.bottom + ACTION_BUTTON_COUNT_VERTICAL_SPACING_PIXELS;
        }

        return Math.min(likeButtonBounds.bottom, dislikeButtonBounds.bottom)
                + ACTION_BUTTON_COUNT_VERTICAL_SPACING_PIXELS;
    }

    /**
     * Positions a count label from a pre-draw callback. View properties are updated only when
     * their values change to avoid requesting another layout and rebinding the EML button row.
     */
    private static void positionRegularActionButtonCountLabel(@NonNull ViewGroup root,
                                                              @NonNull Rect buttonVisualBounds,
                                                              @NonNull TextView textView,
                                                              @NonNull CharSequence text,
                                                              int top,
                                                              int availableWidth,
                                                              float textSizeSp) {
        if (text.length() == 0) {
            textView.setVisibility(View.GONE);
            return;
        }

        CharSequence fittedText = fitRegularActionButtonCountText(
                textView,
                text,
                availableWidth,
                textSizeSp
        );
        if (!TextUtils.equals(textView.getText(), fittedText)) {
            textView.setText(fittedText);
        }
        int textColor = ThemeUtils.getAppForegroundColor();
        if (textView.getCurrentTextColor() != textColor) {
            textView.setTextColor(textColor);
        }
        int rootHeight = Math.max(1, root.getHeight());
        textView.measure(
                View.MeasureSpec.makeMeasureSpec(availableWidth, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(
                        rootHeight + ACTION_BUTTON_COUNT_RESERVED_HEIGHT_PIXELS,
                        View.MeasureSpec.AT_MOST)
        );

        int labelWidth = Math.max(1, textView.getMeasuredWidth());
        textView.measure(
                View.MeasureSpec.makeMeasureSpec(labelWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(
                        rootHeight + ACTION_BUTTON_COUNT_RESERVED_HEIGHT_PIXELS,
                        View.MeasureSpec.AT_MOST)
        );

        int left = buttonVisualBounds.centerX() - labelWidth / 2;

        if (root instanceof FrameLayout) {
            if (textView.getLayoutParams() instanceof FrameLayout.LayoutParams params) {
                if (params.width != labelWidth || params.leftMargin != left || params.topMargin != top) {
                    params.width = labelWidth;
                    params.leftMargin = left;
                    params.topMargin = top;
                    textView.setLayoutParams(params);
                }
            } else {
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        labelWidth,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                );
                params.leftMargin = left;
                params.topMargin = top;
                textView.setLayoutParams(params);
            }
        } else if (textView.getLeft() != left || textView.getTop() != top
                || textView.getRight() != left + labelWidth
                || textView.getBottom() != top + textView.getMeasuredHeight()) {
            textView.layout(left, top, left + labelWidth, top + textView.getMeasuredHeight());
        }

        if (textView.getVisibility() != View.VISIBLE) {
            textView.setVisibility(View.VISIBLE);
            textView.bringToFront();
        }
    }

    /**
     * Returns the horizontal slot available to one count. Paired labels share the distance
     * between button centers, leaving a small gap so localized compact suffixes cannot overlap.
     */
    private static int getRegularActionButtonCountAvailableWidth(int rootWidth,
                                                                 @NonNull Rect buttonVisualBounds,
                                                                 @Nullable Rect adjacentButtonVisualBounds) {
        if (adjacentButtonVisualBounds == null) {
            return rootWidth;
        }

        int centerDistance = Math.abs(
                buttonVisualBounds.centerX() - adjacentButtonVisualBounds.centerX()
        );
        return Math.max(1, Math.min(
                rootWidth,
                centerDistance - ACTION_BUTTON_COUNT_HORIZONTAL_SPACING_PIXELS
        ));
    }

    /**
     * Returns one text size for the count pair. If either localized count needs a smaller size,
     * both labels use it so Like and Dislike remain visually balanced.
     */
    private static float getRegularActionButtonCountTextSizeSp(@Nullable TextView likeLabel,
                                                               @NonNull CharSequence likeText,
                                                               int likeAvailableWidth,
                                                               @Nullable TextView dislikeLabel,
                                                               @NonNull CharSequence dislikeText,
                                                               int dislikeAvailableWidth) {
        return Math.min(
                getRegularActionButtonCountTextSizeSp(likeLabel, likeText, likeAvailableWidth),
                getRegularActionButtonCountTextSizeSp(dislikeLabel, dislikeText, dislikeAvailableWidth)
        );
    }

    private static float getRegularActionButtonCountTextSizeSp(@Nullable TextView textView,
                                                               @NonNull CharSequence text,
                                                               int availableWidth) {
        if (textView == null || text.length() == 0) {
            return ACTION_BUTTON_COUNT_DEFAULT_TEXT_SIZE_SP;
        }

        TextPaint measurementPaint = new TextPaint(textView.getPaint());
        float textSizeSp = ACTION_BUTTON_COUNT_DEFAULT_TEXT_SIZE_SP;
        while (textSizeSp > ACTION_BUTTON_COUNT_MINIMUM_TEXT_SIZE_SP) {
            measurementPaint.setTextSize(TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_SP,
                    textSizeSp,
                    textView.getResources().getDisplayMetrics()
            ));
            if (measurementPaint.measureText(text.toString()) <= availableWidth) {
                break;
            }
            textSizeSp -= ACTION_BUTTON_COUNT_TEXT_SIZE_STEP_SP;
        }
        return textSizeSp;
    }

    /**
     * Applies the shared size selected for the pair, then ellipsizes as a final guard at 9sp.
     */
    @NonNull
    private static CharSequence fitRegularActionButtonCountText(@NonNull TextView textView,
                                                                @NonNull CharSequence text,
                                                                int availableWidth,
                                                                float textSizeSp) {
        float selectedTextSizePixels = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                textSizeSp,
                textView.getResources().getDisplayMetrics()
        );
        if (Math.abs(textView.getTextSize() - selectedTextSizePixels) > 0.5f) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, selectedTextSizePixels);
        }

        TextPaint paint = textView.getPaint();
        return paint.measureText(text.toString()) <= availableWidth
                ? text
                : TextUtils.ellipsize(text, paint, availableWidth, TextUtils.TruncateAt.END);
    }

    @NonNull
    private static Rect getRegularActionButtonVisualBounds(@NonNull ViewGroup root,
                                                           @NonNull View button) {
        int[] rootLocation = new int[2];
        int[] buttonLocation = new int[2];
        root.getLocationInWindow(rootLocation);
        button.getLocationInWindow(buttonLocation);

        float visualScaleX = Math.max(0.1f, Math.min(button.getScaleX(), 1.0f));
        float visualScaleY = Math.max(0.1f, Math.min(button.getScaleY(), 1.0f));
        int visualLeft = buttonLocation[0] - rootLocation[0]
                + Math.round(button.getPivotX() * (1 - visualScaleX));
        int visualTop = buttonLocation[1] - rootLocation[1]
                + Math.round(button.getPivotY() * (1 - visualScaleY));
        int visualWidth = Math.max(1, Math.round(button.getWidth() * visualScaleX));
        int visualHeight = Math.max(1, Math.round(button.getHeight() * visualScaleY));
        return new Rect(visualLeft, visualTop, visualLeft + visualWidth, visualTop + visualHeight);
    }

    private static void removeRegularActionButtonCountOverlays() {
        ViewGroup decorViewRoot = getDecorViewRoot();
        removeRegularActionButtonCountSearchUpdates();
        removeTrackedRegularActionButtonCountLabels();
        removeRegularActionButtonCountPositionUpdates();
        if (decorViewRoot != null) {
            removeRegularActionButtonCountLabels(decorViewRoot);
        }
        restoreRegularActionButtons();
    }

    private static void removeRegularActionButtonCountOverlays(@NonNull ViewGroup root) {
        removeRegularActionButtonCountSearchUpdates();
        removeTrackedRegularActionButtonCountLabels();
        removeRegularActionButtonCountPositionUpdates();
        removeRegularActionButtonCountLabels(root);
        restoreRegularActionButtons();
    }

    private static void removeRegularActionButtonCountLabels(@NonNull ViewGroup root) {
        for (int i = root.getChildCount() - 1; i > -1; i--) {
            View child = root.getChildAt(i);
            if (isRegularActionButtonCountOverlay(child)) {
                root.removeViewAt(i);
            } else if (child instanceof ViewGroup viewGroup) {
                removeRegularActionButtonCountLabels(viewGroup);
            }
        }
    }

    private static void removeTrackedRegularActionButtonCountLabels() {
        removeRegularActionButtonCountLabel(true);
        removeRegularActionButtonCountLabel(false);
    }

    private static void removeRegularActionButtonCountLabel(boolean likeButton) {
        WeakReference<TextView> labelReference = likeButton
                ? regularLikeActionButtonCountLabel
                : regularDislikeActionButtonCountLabel;
        TextView label = labelReference == null ? null : labelReference.get();
        if (label != null && label.getParent() instanceof ViewGroup parent) {
            parent.removeView(label);
        }

        if (likeButton) {
            regularLikeActionButtonCountLabel = null;
        } else {
            regularDislikeActionButtonCountLabel = null;
        }
    }

    @Nullable
    private static ViewGroup getDecorViewRoot() {
        Activity activity = Utils.getActivity();
        if (activity == null || !(activity.getWindow().getDecorView() instanceof ViewGroup root)) {
            return null;
        }
        return root;
    }

    private record RegularActionButtonAnchors(@Nullable View likeButton,
                                              @Nullable View dislikeButton) {
        @NonNull
        private View getVisibleButton() {
            return likeButton != null ? likeButton : Objects.requireNonNull(dislikeButton);
        }
    }

    private static void hideTrackedRegularActionButtonCountLabels() {
        TextView likeLabel = regularLikeActionButtonCountLabel == null
                ? null
                : regularLikeActionButtonCountLabel.get();
        TextView dislikeLabel = regularDislikeActionButtonCountLabel == null
                ? null
                : regularDislikeActionButtonCountLabel.get();

        if (likeLabel != null) {
            likeLabel.setVisibility(View.GONE);
        }
        if (dislikeLabel != null) {
            dislikeLabel.setVisibility(View.GONE);
        }
    }

    private static boolean isRegularActionButtonCountOverlay(@NonNull View view) {
        Object tag = view.getTag();
        return tag instanceof String && ((String) tag).startsWith(ACTION_BUTTON_COUNT_TAG_PREFIX);
    }

    @NonNull
    private static String getRegularActionButtonCountOverlayTag(boolean likeButton) {
        return ACTION_BUTTON_COUNT_TAG_PREFIX + (likeButton ? "like" : "dislike");
    }

    private static boolean isVisibleOnScreen(@NonNull View view) {
        if (!view.isShown() || view.getWidth() <= 0 || view.getHeight() <= 0) {
            return false;
        }

        Rect windowRect = new Rect();
        view.getWindowVisibleDisplayFrame(windowRect);
        Rect bounds = getViewWindowBounds(view);
        return Rect.intersects(windowRect, bounds);
    }

    @NonNull
    private static Rect getViewWindowBounds(@NonNull View view) {
        int[] location = new int[2];
        view.getLocationInWindow(location);
        return new Rect(location[0], location[1],
                location[0] + view.getWidth(), location[1] + view.getHeight());
    }

    private record RegularActionButtonViewState(float scaleX, float scaleY, float pivotX, float pivotY) {
    }

    //
    // Non-litho Shorts player.
    //

    /**
     * Replacement text to use for "Dislikes" while RYD is fetching.
     */
    private static final Spannable SHORTS_LOADING_SPAN = new SpannableString("-");

    /**
     * Dislikes TextViews used by Shorts.
     * <p>
     * Multiple TextViews are loaded at once (for the prior and next videos to swipe to).
     * Keep track of all of them, and later pick out the correct one based on their on-screen position.
     */
    private static final List<WeakReference<TextView>> shortsTextViewRefs = new ArrayList<>();

    private static void clearRemovedShortsTextViews() {
        shortsTextViewRefs.removeIf(ref -> ref.get() == null);
    }

    /**
     * Injection point.  Called when a Shorts dislike is updated.  Always on main thread.
     * Handles update asynchronously, otherwise Shorts video will be frozen while the UI thread is blocked.
     *
     * @return if RYD is enabled and the TextView was updated.
     */
    public static boolean setShortsDislikes(@NonNull View likeDislikeView) {
        try {
            if (!Settings.RYD_ENABLED.get()) {
                return false;
            }
            if (!Settings.RYD_SHORTS.get() || Settings.HIDE_SHORTS_DISLIKE_BUTTON.get()) {
                // Must clear the data here, in case a new video was loaded while PlayerType
                // suggested the video was not a short (can happen when spoofing to an old app version).
                clearData();
                return false;
            }
            Logger.printDebug(() -> "setShortsDislikes");

            TextView textView = (TextView) likeDislikeView;
            textView.setText(SHORTS_LOADING_SPAN); // Change 'Dislike' text to the loading text.
            shortsTextViewRefs.add(new WeakReference<>(textView));

            if (likeDislikeView.isSelected() && isShortTextViewOnScreen(textView)) {
                Logger.printDebug(() -> "Shorts dislike is already selected");
                ReturnYouTubeDislike videoData = currentVideoData;
                if (videoData != null) videoData.setUserVote(Vote.DISLIKE);
            }

            // For the first short played, the Shorts dislike hook is called after the video id hook.
            // But for most other times this hook is called before the video id (which is not ideal).
            // Must update the TextViews here, and also after the videoId changes.
            updateOnScreenShortsTextViews(false);

            return true;
        } catch (Exception ex) {
            Logger.printException(() -> "setShortsDislikes failure", ex);
            return false;
        }
    }

    /**
     * @param forceUpdate if false, then only update the loading text views.
     *                    If true, update all on-screen text views.
     */
    private static void updateOnScreenShortsTextViews(boolean forceUpdate) {
        try {
            clearRemovedShortsTextViews();
            if (shortsTextViewRefs.isEmpty()) {
                return;
            }
            ReturnYouTubeDislike videoData = currentVideoData;
            if (videoData == null) {
                return;
            }

            Logger.printDebug(() -> "updateShortsTextViews");

            Runnable update = () -> {
                Spanned shortsDislikesSpan = videoData.getDislikeSpanForShort(SHORTS_LOADING_SPAN);
                Utils.runOnMainThreadNowOrLater(() -> {
                    String videoId = videoData.getVideoId();
                    if (!videoId.equals(VideoInformation.getVideoId())) {
                        // User swiped to new video before fetch completed
                        Logger.printDebug(() -> "Ignoring stale dislikes data for short: " + videoId);
                        return;
                    }

                    // Update text views that appear to be visible on screen.
                    // Only 1 will be the actual textview for the current Short,
                    // but discarded and not yet garbage collected views can remain.
                    // So must set the dislike span on all views that match.
                    for (WeakReference<TextView> textViewRef : shortsTextViewRefs) {
                        TextView textView = textViewRef.get();
                        if (textView == null) {
                            continue;
                        }
                        if (isShortTextViewOnScreen(textView)
                                && (forceUpdate || textView.getText().toString().equals(SHORTS_LOADING_SPAN.toString()))) {
                            Logger.printDebug(() -> "Setting Shorts TextView to: " + shortsDislikesSpan);
                            textView.setText(shortsDislikesSpan);
                        }
                    }
                });
            };
            if (videoData.fetchCompleted()) {
                update.run(); // Network call is completed, no need to wait on background thread.
            } else {
                Utils.runOnBackgroundThread(update);
            }
        } catch (Exception ex) {
            Logger.printException(() -> "updateOnScreenShortsTextViews failure", ex);
        }
    }

    /**
     * Check if a view is within the screen bounds.
     */
    private static boolean isShortTextViewOnScreen(@NonNull View view) {
        final int[] location = new int[2];
        view.getLocationInWindow(location);
        if (location[0] <= 0 && location[1] <= 0) { // Lower bound
            return false;
        }
        Rect windowRect = new Rect();
        view.getWindowVisibleDisplayFrame(windowRect); // Upper bound
        return location[0] < windowRect.width() && location[1] < windowRect.height();
    }


    //
    // Video ID and voting hooks (all players).
    //

    private static volatile boolean lastPlayerResponseWasShort;

    /**
     * Video ads in the regular video player are not hooked to {@link #preloadVideoId(String, boolean)}.
     * Therefore, video ads in the regular video player are not prefetched.
     * This can be used to identify whether a video id is a video ad or not.
     */
    @GuardedBy("itself")
    private static final Map<String, Boolean> playerResponseVideoIds = new LinkedHashMap<>() {
        private static final int NUMBER_OF_LAST_VIDEO_IDS_TO_TRACK = 5;

        @Override
        protected boolean removeEldestEntry(Entry eldest) {
            return size() > NUMBER_OF_LAST_VIDEO_IDS_TO_TRACK;
        }
    };

    /**
     * Injection point.  Uses 'playback response' video id hook to preload RYD.
     */
    public static void preloadVideoId(@NonNull String videoId, boolean isShortAndOpeningOrPlaying) {
        try {
            if (!Settings.RYD_ENABLED.get()) {
                return;
            }
            if (videoId.equals(lastPrefetchedVideoId)) {
                return;
            }
            final boolean videoIdIsShort = VideoInformation.lastPlayerResponseIsShort();
            synchronized (playerResponseVideoIds) {
                // Add all prefetched video ids to 'playerResponseVideoIds'.
                playerResponseVideoIds.putIfAbsent(videoId, videoIdIsShort);
            }
            if (!Utils.isNetworkConnected()) {
                Logger.printDebug(() -> "Cannot pre-fetch RYD, network is not connected");
                lastPrefetchedVideoId = null;
                return;
            }

            // Shorts shelf in home and subscription feed causes player response hook to be called,
            // and the 'is opening/playing' parameter will be false.
            // This hook will be called again when the Short is actually opened.
            if (videoIdIsShort && (!isShortAndOpeningOrPlaying || !Settings.RYD_SHORTS.get())) {
                return;
            }
            final boolean waitForFetchToComplete = !IS_SPOOFING_TO_NON_LITHO_SHORTS_PLAYER
                    && videoIdIsShort && !lastPlayerResponseWasShort;

            Logger.printDebug(() -> "Prefetching RYD for video: " + videoId);
            ReturnYouTubeDislike fetch = ReturnYouTubeDislike.getFetchForVideoId(videoId);
            if (waitForFetchToComplete && !fetch.fetchCompleted()) {
                // This call is off the main thread, so wait until the RYD fetch completely finishes,
                // otherwise if this returns before the fetch completes then the UI can
                // become frozen when the main thread tries to modify the litho Shorts dislikes, and
                // it must wait for the fetch.
                // Only need to do this for the first Short opened, as the next Short to swipe to
                // are preloaded in the background.
                //
                // If an asynchronous litho Shorts solution is found, then this blocking call should be removed.
                Logger.printDebug(() -> "Waiting for prefetch to complete: " + videoId);
                fetch.getFetchData(20000); // Any arbitrarily large max wait time.
            }

            // Set the fields after the fetch completes, so any concurrent calls will also wait.
            lastPlayerResponseWasShort = videoIdIsShort;
            lastPrefetchedVideoId = videoId;
        } catch (Exception ex) {
            Logger.printException(() -> "preloadVideoId failure", ex);
        }
    }

    /**
     * Injection point.  Uses 'current playing' video id hook.  Always called on main thread.
     */
    public static void newVideoLoaded(@NonNull String videoId) {
        try {
            if (!Settings.RYD_ENABLED.get()) {
                return;
            }
            Objects.requireNonNull(videoId);

            final PlayerType currentPlayerType = PlayerType.getCurrent();
            final boolean isNoneHiddenOrSlidingMinimized = currentPlayerType.isNoneHiddenOrSlidingMinimized();
            if (isNoneHiddenOrSlidingMinimized && !Settings.RYD_SHORTS.get()) {
                // Must clear here, otherwise the wrong data can be used for a minimized regular video.
                clearData();
                return;
            }

            if (videoIdIsSame(currentVideoData, videoId)) {
                return;
            }
            synchronized (playerResponseVideoIds) {
                // All video ids except those in the regular video player have been prefetched.
                // Video ids not present in 'playerResponseVideoIds' are video ads from the regular video player.
                if (playerResponseVideoIds.get(videoId) == null) {
                    // When a regular video player video ad is fetched,
                    // the dislike count of the video ad is used instead of the dislike count of the original video.
                    Logger.printDebug(() -> "Skip video ads: " + videoId);
                    return;
                }
            }

            if (!Utils.isNetworkConnected()) {
                Logger.printDebug(() -> "Cannot fetch RYD, network is not connected");
                currentVideoData = null;
                return;
            }

            Logger.printDebug(() -> "New video id: " + videoId + " playerType: " + currentPlayerType);

            ReturnYouTubeDislike data = ReturnYouTubeDislike.getFetchForVideoId(videoId);
            // Pre-emptively set the data to short status.
            // Required to prevent Shorts data from being used on a minimized video in incognito mode.
            if (isNoneHiddenOrSlidingMinimized) {
                data.setVideoIdIsShort(true);
            }
            currentVideoData = data;

            // Current video id hook can be called out of order with the non-litho Shorts text view hook.
            // Must manually update again here.
            if (isNoneHiddenOrSlidingMinimized) {
                updateOnScreenShortsTextViews(true);
            } else if (canShowRegularActionButtonCountOverlays(currentPlayerType)) {
                scheduleRegularActionButtonCountOverlayUpdates();
            } else {
                removeRegularActionButtonCountOverlays();
            }
        } catch (Exception ex) {
            Logger.printException(() -> "newVideoLoaded failure", ex);
        }
    }

    public static void setLastLithoShortsVideoId(@Nullable String videoId) {
        if (videoIdIsSame(lastLithoShortsVideoData, videoId)) {
            return;
        }

        if (videoId == null) {
            // Litho filter did not detect the video id.  App is in incognito mode,
            // or the proto buffer structure was changed and the video id is no longer present.
            // Must clear both currently playing and last litho data otherwise the
            // next regular video may use the wrong data.
            Logger.printDebug(() -> "Litho filter did not find any video ids");
            clearData();
            return;
        }

        Logger.printDebug(() -> "New litho Shorts video id: " + videoId);
        ReturnYouTubeDislike videoData = ReturnYouTubeDislike.getFetchForVideoId(videoId);
        videoData.setVideoIdIsShort(true);
        lastLithoShortsVideoData = videoData;
        synchronized (ReturnYouTubeDislikePatch.class) {
            // Use litho Shorts data for the next like and dislike spans.
            useLithoShortsVideoDataCount = 2;
        }
    }

    private static boolean videoIdIsSame(@Nullable ReturnYouTubeDislike fetch, @Nullable String videoId) {
        return (fetch == null && videoId == null)
                || (fetch != null && fetch.getVideoId().equals(videoId));
    }

    /**
     * Injection point.
     * <p>
     * Called when the user likes or dislikes.
     *
     * @param endpoint string that matches {@link Vote#endpoint}
     * @param videoId  video ID included in the endpoint request body
     */
    public static void sendVote(@NonNull String endpoint, @Nullable String videoId) {
        try {
            if (!Settings.RYD_ENABLED.get()) {
                return;
            }
            if (videoId == null || videoId.isEmpty()) {
                Logger.printDebug(() -> "Ignore playlist votes");
                return;
            }

            final boolean isNoneHiddenOrMinimized = PlayerType.getCurrent().isNoneHiddenOrMinimized();
            if (isNoneHiddenOrMinimized && !Settings.RYD_SHORTS.get()) {
                return;
            }

            ReturnYouTubeDislike videoData = currentVideoData;
            if (videoData == null) {
                Logger.printDebug(() -> "Cannot send vote, as current video data is null");
                return; // User enabled RYD while a regular video was minimized.
            } else if (!videoIdIsSame(videoData, videoId)) {
                Logger.printDebug(() -> "Cannot vote for video, as video id does not match"
                        + " videoData: " + videoData.getVideoId() + ", endpoint: " + videoId);
                return;
            }

            for (Vote v : Vote.values()) {
                if (v.endpoint.equals(endpoint)) {
                    videoData.sendVote(v);
                    regularActionButtonCountVideoId = null;
                    regularLikeActionButtonCountText = null;
                    regularDislikeActionButtonCountText = null;
                    scheduleRegularActionButtonCountOverlayUpdates();
                    return;
                }
            }
            Logger.printException(() -> "Unknown endpoint: " + endpoint);
        } catch (Exception ex) {
            Logger.printException(() -> "sendVote failure", ex);
        }
    }

    /**
     * Parses the original like count from the like button's accessibility content description.
     * <p>
     * Instead of using fragile regex patterns or checking language-specific suffixes, this method
     * implements a completely universal digits-only parsing algorithm:
     * 1. Finds the first digit in the string.
     * 2. Extracts all digit characters, ignoring separators (like commas, dots, and spaces).
     * 3. Normalizes Unicode digits (like Arabic-Indic digits) to standard ASCII 0-9 values.
     * 4. Parses the resulting string directly into a Long value.
     * This is robust because accessibility guidelines require screen readers to receive the full,
     * un-compactified count (e.g. 5,497,262 instead of 5.5M), and the likes count is the only
     * variable number present in the like button's content description.
     * <p>
     * The contentDescription always indicates that the video is already liked and never changes,
     * even after the user toggles the like button.
     * This causes issues with the like count, particularly after restarting the app.
     *
     * @param contentDescription The raw content description of the like button.
     * @return The extracted likes count, or null if parsing failed.
     */
    @Nullable
    private static Long tryParseLikesFromContentDescription(@Nullable CharSequence contentDescription) {
        if (contentDescription == null) {
            Logger.printDebug(() -> "tryParseLikesFromContentDescription: contentDescription is null");
            return null;
        }
        String desc = contentDescription.toString();
        Logger.printDebug(() -> "tryParseLikesFromContentDescription: contentDescription='" + desc + "'");
        
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < desc.length(); i++) {
            char c = desc.charAt(i);
            if (Character.isDigit(c)) {
                int digitVal = Character.getNumericValue(c);
                if (digitVal >= 0 && digitVal <= 9) {
                    digits.append(digitVal);
                }
            }
        }
        
        if (digits.length() == 0) {
            Logger.printDebug(() -> "tryParseLikesFromContentDescription: No digits found");
            return null;
        }
        
        try {
            Long val = Long.parseLong(digits.toString());
            Logger.printDebug(() -> "tryParseLikesFromContentDescription: parsed val=" + val);
            return val;
        } catch (NumberFormatException e) {
            Logger.printDebug(() -> "tryParseLikesFromContentDescription: Failed to parse digits='" + digits + "'");
            return null;
        }
    }

    private static void parseAndSetOriginalLikeCount(@Nullable CharSequence contentDesc) {
        Long parsedLikes = tryParseLikesFromContentDescription(contentDesc);
        if (parsedLikes != null) {
            long baseCount = parsedLikes - 1;
            VideoInformation.setOriginalLikeCount(Math.max(0L, baseCount), true);
        }
    }

    private static boolean isLikeButtonLiked(@NonNull View likeButton) {
        if (likeButton.isSelected()) {
            return true;
        }
        CharSequence contentDescription = likeButton.getContentDescription();
        if (contentDescription == null) {
            return false;
        }
        cacheContentDescriptionStrings();
        String description = contentDescription.toString().toLowerCase(Locale.ROOT);
        if (cachedUndoLikeStr != null && description.contains(cachedUndoLikeStr)) {
            return true;
        }
        return description.contains("undo like");
    }

    private static boolean isDislikeButtonDisliked(@NonNull View dislikeButton) {
        if (dislikeButton.isSelected()) {
            return true;
        }
        CharSequence contentDescription = dislikeButton.getContentDescription();
        if (contentDescription == null) {
            return false;
        }
        cacheContentDescriptionStrings();
        String description = contentDescription.toString().toLowerCase(Locale.ROOT);
        if (cachedUndoDislikeStr != null && description.contains(cachedUndoDislikeStr)) {
            return true;
        }
        return description.contains("undo dislike");
    }
}
