package app.revanced.extension.youtube.patches.utils;

import static app.revanced.extension.shared.utils.ResourceUtils.getIdIdentifier;

import android.view.View;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;

import app.revanced.extension.shared.utils.Utils;
import app.revanced.extension.youtube.shared.PlayerControlsVisibility;

/**
 * @noinspection ALL
 */
public class PlayerControlsPatch {
    private static WeakReference<View> playerOverflowButtonViewRef = new WeakReference<>(null);
    private static final int playerOverflowButtonId =
            getIdIdentifier("player_overflow_button");

    /**
     * Injection point.
     */
    public static void initializeBottomControlButton(View bottomControlsViewGroup) {
        // AlwaysRepeat.initialize(bottomControlsViewGroup);
        // CopyVideoUrl.initialize(bottomControlsViewGroup);
        // CopyVideoUrlTimestamp.initialize(bottomControlsViewGroup);
        // MuteVolume.initialize(bottomControlsViewGroup);
        // ExternalDownload.initialize(bottomControlsViewGroup);
        // SpeedDialog.initialize(bottomControlsViewGroup);
        // TimeOrderedPlaylist.initialize(bottomControlsViewGroup);
        // Whitelists.initialize(bottomControlsViewGroup);
    }

    /**
     * Injection point.
     */
    public static void initializeTopControlButton(View youtubeControlsLayout) {
        // CreateSegmentButtonController.initialize(youtubeControlsLayout);
        // VotingButtonController.initialize(youtubeControlsLayout);
    }

    /**
     * Injection point.
     * Legacy method.
     * <p>
     * Player overflow button view does not attach to windows immediately after cold start.
     * Player overflow button view is not attached to the windows until the user touches the player at least once, and the overlay buttons are hidden until then.
     * To prevent this, uses the legacy method to show the overlay button until the player overflow button view is attached to the windows.
     */
    public static void changeVisibility(boolean showing) {
        if (playerOverflowButtonViewRef.get() != null) {
            return;
        }
        changeVisibility(showing, false);
    }

    private static void changeVisibility(boolean showing, boolean animation) {
        // AlwaysRepeat.changeVisibility(showing, animation);
        // CopyVideoUrl.changeVisibility(showing, animation);
        // CopyVideoUrlTimestamp.changeVisibility(showing, animation);
        // MuteVolume.changeVisibility(showing, animation);
        // ExternalDownload.changeVisibility(showing, animation);
        // SpeedDialog.changeVisibility(showing, animation);
        // TimeOrderedPlaylist.changeVisibility(showing, animation);
        // Whitelists.changeVisibility(showing, animation);

        // CreateSegmentButtonController.changeVisibility(showing, animation);
        // VotingButtonController.changeVisibility(showing, animation);
    }

    /**
     * Injection point.
     * New method.
     * <p>
     * Show or hide the overlay button when the player overflow button view is visible and hidden, respectively.
     * <p>
     * Inject the current view into {@link PlayerControlsPatch#playerOverflowButtonView} to check that the player overflow button view is attached to the window.
     * From this point on, the legacy method is deprecated.
     */
    public static void changeVisibility(boolean showing, boolean animation, @NonNull View view) {
        if (view.getId() != playerOverflowButtonId) {
            return;
        }
        if (playerOverflowButtonViewRef.get() == null) {
            Utils.runOnMainThreadDelayed(() -> playerOverflowButtonViewRef = new WeakReference<>(view), 1400);
        }
        changeVisibility(showing, animation);
    }

    /**
     * Injection point.
     * <p>
     * Called whenever a motion event occurs on the player controller.
     * <p>
     * When the user touches the player overlay (motion event occurs), the player overlay disappears immediately.
     * In this case, the overlay buttons should also disappear immediately.
     * <p>
     * In other words, this method detects when the player overlay disappears immediately upon the user's touch,
     * and quickly fades out all overlay buttons.
     */
    public static void changeVisibilityNegatedImmediate() {
        if (PlayerControlsVisibility.getCurrent() == PlayerControlsVisibility.PLAYER_CONTROLS_VISIBILITY_HIDDEN) {
            changeVisibilityNegatedImmediately();
        }
    }

    private static void changeVisibilityNegatedImmediately() {
        // AlwaysRepeat.changeVisibilityNegatedImmediate();
        // CopyVideoUrl.changeVisibilityNegatedImmediate();
        // CopyVideoUrlTimestamp.changeVisibilityNegatedImmediate();
        // MuteVolume.changeVisibilityNegatedImmediate();
        // ExternalDownload.changeVisibilityNegatedImmediate();
        // SpeedDialog.changeVisibilityNegatedImmediate();
        // TimeOrderedPlaylist.changeVisibilityNegatedImmediate();
        // Whitelists.changeVisibilityNegatedImmediate();

        // CreateSegmentButtonController.changeVisibilityNegatedImmediate();
        // VotingButtonController.changeVisibilityNegatedImmediate();
    }

    /**
     * Injection point.
     */
    public static String getPlayerTopControlsLayoutResourceName(String original) {
        return "default";
    }
}
