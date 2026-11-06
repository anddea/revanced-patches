/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
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

package app.morphe.extension.youtube.sponsorblock;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.ResourceUtils;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.shared.RootView;
import app.morphe.extension.youtube.shared.ShortsPlayerState;
import app.morphe.extension.youtube.shared.VideoInformation;
import app.morphe.extension.youtube.sponsorblock.objects.SponsorSegment;
import app.morphe.extension.youtube.sponsorblock.ui.SkipSponsorButton;
import kotlin.Unit;

/**
 * Uses the visible Shorts timebar's scrub-completion callback to seek the active player.
 * The regular playback controller may belong to a preloaded Short instead.
 */
@SuppressWarnings("unused")
public final class ShortsSponsorBlockController {
    /** Implemented on YouTube's shared seekbar by the bytecode patch. */
    public interface Seekbar {
        void patch_seekTo(long time);
        boolean patch_isScrubbing();
    }

    private static final int reelTimeBarId = ResourceUtils.getIdIdentifier("reel_time_bar");
    private static WeakReference<View> seekbarRef = new WeakReference<>(null);
    private static WeakReference<SkipSponsorButton> skipButtonRef = new WeakReference<>(null);
    private static String seekbarVideoId = "";
    @Nullable
    private static SponsorSegment skipSegment;
    @Nullable
    private static SponsorSegment skipHighlight;

    static {
        ShortsPlayerState.getOnChange().addObserver(state -> {
            if (state.isClosed()) {
                clear();
            }
            return Unit.INSTANCE;
        });
    }

    private ShortsSponsorBlockController() {
    }

    /** Injection point, shared by the regular and Shorts seekbars. */
    public static void setSeekbar(View view) {
        if (!RootView.isShortsActive() || !VideoInformation.lastVideoIdIsShort() || !view.isShown()) {
            return;
        }

        View ancestor = view;
        while (ancestor.getId() != reelTimeBarId) {
            if (!(ancestor.getParent() instanceof View)) return;
            ancestor = (View) ancestor.getParent();
        }

        String videoId = SegmentPlaybackController.getVideoId();
        boolean changed = seekbarRef.get() != view || !seekbarVideoId.equals(videoId);
        seekbarRef = new WeakReference<>(view);
        seekbarVideoId = videoId;
        if (changed && (skipSegment != null || skipHighlight != null)) {
            // Adding a child while the parent is drawing is unsafe.
            view.post(ShortsSponsorBlockController::updateSkipButton);
        }
    }

    /** Identifies the current Shorts bar without excluding its scrubbing state from drawing. */
    public static boolean isCurrentSeekbar(@Nullable View view) {
        return RootView.isShortsActive() && view != null && view == seekbarRef.get() && view.isShown()
                && !seekbarVideoId.isEmpty()
                && seekbarVideoId.equals(SegmentPlaybackController.getVideoId())
                && seekbarVideoId.equals(VideoInformation.getVideoId());
    }

    /** Never seek a stale or actively scrubbed view when the user changes Shorts. */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean canSeek() {
        View view = seekbarRef.get();
        return isCurrentSeekbar(view) && SegmentPlaybackController.getVideoLength() > 0
                && !((Seekbar) view).patch_isScrubbing();
    }

    @Nullable
    public static Context getPlayerContext() {
        View view = seekbarRef.get();
        return isCurrentSeekbar(view) ? view.getContext() : null;
    }

    public static boolean seekTo(long time) {
        Utils.verifyOnMainThread();
        if (!canSeek()) return false;

        View view = seekbarRef.get();
        try {
            ((Seekbar) view).patch_seekTo(Math.max(0, Math.min(time,
                    Math.max(0, SegmentPlaybackController.getVideoLength() - 1))));
            return true;
        } catch (Exception ex) {
            Logger.printException(() -> "Could not seek Short", ex);
            return false;
        }
    }

    /** Reuses the existing skip button and category text for manually skipped Shorts. */
    public static void showSkipButton(@Nullable SponsorSegment segment) {
        skipSegment = segment;
        updateSkipButton();
    }

    public static void showHighlightButton(@Nullable SponsorSegment segment) {
        skipHighlight = segment;
        updateSkipButton();
    }

    private static void updateSkipButton() {
        SponsorSegment segment = skipSegment != null ? skipSegment : skipHighlight;
        SkipSponsorButton button = skipButtonRef.get();

        if (segment == null || !RootView.isShortsActive()) {
            if (button != null) button.setVisibility(View.GONE);
            return;
        }

        View seekbar = seekbarRef.get();
        if (seekbar == null || !seekbar.isShown()) return;

        try {
            View content = seekbar.getRootView().findViewById(android.R.id.content);
            if (!(content instanceof FrameLayout parent)) return;

            if (button == null || button.getParent() != parent) {
                if (button != null && button.getParent() instanceof ViewGroup) {
                    ((ViewGroup) button.getParent()).removeView(button);
                }

                button = new SkipSponsorButton(seekbar.getContext());
                float density = seekbar.getResources().getDisplayMetrics().density;
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                        Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);

                params.setMargins(0, 0, 0, Math.round(128 * density));
                parent.addView(button, params);
                skipButtonRef = new WeakReference<>(button);
            }

            button.updateSkipButtonText(segment);
            button.setVisibility(View.VISIBLE);
            button.bringToFront();
        } catch (Exception ex) {
            Logger.printException(() -> "Could not show Shorts skip button", ex);
        }
    }

    /** Invalidates view ownership and removes pending manual skips when playback changes. */
    public static void clear() {
        seekbarVideoId = "";
        skipHighlight = null;
        skipSegment = null;
        Utils.runOnMainThreadNowOrLater(ShortsSponsorBlockController::updateSkipButton);
    }
}
