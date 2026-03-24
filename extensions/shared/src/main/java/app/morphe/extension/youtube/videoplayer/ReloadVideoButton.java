package app.morphe.extension.youtube.videoplayer;

import android.view.View;

import androidx.annotation.Nullable;

import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.youtube.patches.ReloadVideoPatch;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.shared.PlayerControlButton;
import app.morphe.extension.youtube.shared.RootView;

@SuppressWarnings("unused")
public class ReloadVideoButton {
    @Nullable
    private static PlayerControlButton instance;

    /**
     * Injection point.
     */
    public static void initializeButton(View controlsView) {
        try {
            instance = new PlayerControlButton(
                    controlsView,
                    "morphe_reload_video_button",
                    false,
                    () -> Settings.RELOAD_VIDEO.get() && !RootView.isAdProgressTextVisible(),
                    v -> ReloadVideoPatch.reloadVideo(),
                    null
            );
        } catch (Exception ex) {
            Logger.printException(() -> "initialize failure", ex);
        }
    }

    /**
     * Injection point.
     */
    public static void setVisibilityNegatedImmediate() {
        if (instance != null) instance.setVisibilityNegatedImmediate();
    }

    /**
     * Injection point.
     */
    public static void setVisibilityImmediate(boolean visible) {
        if (instance != null) instance.setVisibilityImmediate(visible);
    }

    /**
     * Injection point.
     */
    public static void setVisibility(boolean visible, boolean animated) {
        if (instance != null) instance.setVisibility(visible, animated);
    }
}
