/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/4840
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.extension.youtube.patches.dearrow;

import static app.morphe.extension.shared.utils.StringRef.str;

import android.view.View;

import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.shared.PlayerControlButton;

/**
 * Button at the top of the player that opens the DeArrow titles and thumbnails of the video,
 * to vote on them or submit new ones.
 */
@SuppressWarnings("unused")
public final class DeArrowVoteButton {

    private static final String BUTTON_ID = "morphe_dearrow_vote_button";

    private static PlayerControlButton instance;

    private DeArrowVoteButton() {
    }

    /**
     * Injection point.
     */
    public static void initializeButton(View controlsView) {
        try {
            instance = new PlayerControlButton(
                    controlsView,
                    BUTTON_ID,
                    false,
                    Settings.DEARROW_VOTE_BUTTON::get,
                    DeArrowVoteButton::showDialog,
                    null
            );
            instance.setIcon("morphe_yt_dearrow");
            Utils.getChildViewByResourceName(controlsView, BUTTON_ID)
                    .setContentDescription(str("morphe_dearrow_vote_title"));
        } catch (Exception ex) {
            Logger.printException(() -> "initializeButton failure", ex);
        }
    }

    /** Injection point. */
    public static void setVisibilityNegatedImmediate() {
        if (instance != null) instance.setVisibilityNegatedImmediate();
    }

    /** Injection point. */
    public static void setVisibilityImmediate(boolean visible) {
        if (instance != null) instance.setVisibilityImmediate(visible);
    }

    /** Injection point. */
    public static void setVisibility(boolean visible, boolean animated) {
        if (instance != null) instance.setVisibility(visible, animated);
    }

    private static void showDialog(View view) {
        try {
            DeArrowVoteDialog.show(view);
        } catch (Exception ex) {
            Logger.printException(() -> "showDialog failure", ex);
        }
    }
}
