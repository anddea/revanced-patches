package app.revanced.extension.youtube.patches.overlaybutton;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.utils.VideoUtils;

@SuppressWarnings("unused")
public class PlayAll extends BottomControlButton {

    @Nullable
    private static PlayAll instance;

    public PlayAll(ViewGroup bottomControlsViewGroup) {
        super(
                bottomControlsViewGroup,
                "play_all_button",
                Settings.OVERLAY_BUTTON_PLAY_ALL,
                view -> VideoUtils.openVideo(Settings.OVERLAY_BUTTON_PLAY_ALL_TYPE.get()),
                view -> {
                    VideoUtils.openVideo();
                    return true;
                }
        );
    }

    /**
     * Injection point.
     */
    public static void initialize(View bottomControlsViewGroup) {
        try {
            if (bottomControlsViewGroup instanceof ViewGroup viewGroup) {
                instance = new PlayAll(viewGroup);
            }
        } catch (Exception ex) {
            Logger.printException(() -> "initialize failure", ex);
        }
    }

    /**
     * Injection point.
     */
    public static void changeVisibility(boolean showing, boolean animation) {
        if (instance != null) instance.setVisibility(showing, animation);
    }

    public static void changeVisibilityNegatedImmediate() {
        if (instance != null) instance.setVisibilityNegatedImmediate();
    }

}