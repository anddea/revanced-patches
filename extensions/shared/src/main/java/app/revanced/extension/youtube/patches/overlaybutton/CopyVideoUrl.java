package app.revanced.extension.youtube.patches.overlaybutton;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.utils.VideoUtils;

@SuppressWarnings("unused")
public class CopyVideoUrl extends BottomControlButton {
    @Nullable
    private static CopyVideoUrl instance;

    public CopyVideoUrl(ViewGroup bottomControlsViewGroup) {
        super(
                bottomControlsViewGroup,
                "copy_video_url_button",
                Settings.OVERLAY_BUTTON_COPY_VIDEO_URL,
                view -> VideoUtils.copyUrl(false),
                view -> {
                    VideoUtils.copyUrl(true);
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
                instance = new CopyVideoUrl(viewGroup);
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