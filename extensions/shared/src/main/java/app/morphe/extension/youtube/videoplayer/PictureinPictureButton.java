/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/3397
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.extension.youtube.videoplayer;

import static app.morphe.extension.shared.utils.StringRef.str;
import static app.morphe.extension.youtube.settings.YouTubeActivityHook.USE_BOLD_ICONS;

import android.graphics.drawable.Drawable;
import android.view.View;

import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.ResourceUtils;
import app.morphe.extension.youtube.shared.PlayerControlButton;
import app.morphe.extension.youtube.patches.PictureinPictureButtonPatch;
import app.morphe.extension.youtube.patches.utils.FlyoutUtils;
import app.morphe.extension.youtube.settings.Settings;

/**
 * Uses the shared top controls and YouTube's thin/bold icon selection, as upstream does.
 * PiP is independent of the bottom overlay button icon style.
 */
@SuppressWarnings("unused")
public class PictureinPictureButton {

    private static final String pipButtonName = str("morphe_pip_button_flyout_name");

    private static PlayerControlButton instance;

    /**
     * Injection point.
     */
    public static void initializeButton(View controlsView) {
        try {
            instance = new PlayerControlButton(
                    controlsView,
                    "morphe_pip_button",
                    false,
                    () -> Settings.PIP_OVERLAY_BUTTON.get() && PictureinPictureButtonPatch.isPipSupported(),
                    v -> PictureinPictureButtonPatch.enterPictureInPicture(),
                    null
            );
            instance.setIcon("morphe_pip_button");
        } catch (Exception ex) {
            Logger.printException(() -> "initialize failure", ex);
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

    public static int addFlyoutButton(Object flyoutPanel, int nextButtonIndex, boolean isTopFlyout, boolean isShortFlyout) {
        if (Settings.PIP_FLYOUT_BUTTON.get() && (isTopFlyout || isShortFlyout)) {

            Drawable icon = ResourceUtils.getDrawable(USE_BOLD_ICONS
                    ? "morphe_pip_button_bold"
                    : "morphe_pip_button");

            nextButtonIndex = FlyoutUtils.addFlyoutButton(
                    flyoutPanel,
                    icon,
                    pipButtonName,
                    v -> PictureinPictureButtonPatch.enterPictureInPicture(),
                    nextButtonIndex
            );
        }
        return nextButtonIndex;
    }
}
