package app.revanced.extension.youtube.patches.overlaybutton;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.utils.VideoUtils;

@SuppressWarnings("unused")
public class CopyVideoUrlTimestamp extends BottomControlButton {
    @Nullable
    private static CopyVideoUrlTimestamp instance;

    public CopyVideoUrlTimestamp(ViewGroup bottomControlsViewGroup) {
        super(
                bottomControlsViewGroup,
                "copy_video_url_timestamp_button",
                Settings.OVERLAY_BUTTON_COPY_VIDEO_URL_TIMESTAMP,
                view -> VideoUtils.copyUrl(true),
                view -> {
                    VideoUtils.copyTimeStamp();
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
                instance = new CopyVideoUrlTimestamp(viewGroup);
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