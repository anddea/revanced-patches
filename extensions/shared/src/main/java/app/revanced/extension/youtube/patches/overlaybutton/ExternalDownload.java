package app.revanced.extension.youtube.patches.overlaybutton;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.utils.VideoUtils;

@SuppressWarnings("unused")
public class ExternalDownload extends BottomControlButton {
    @Nullable
    private static ExternalDownload instance;

    public ExternalDownload(ViewGroup bottomControlsViewGroup) {
        super(
                bottomControlsViewGroup,
                "external_download_button",
                Settings.OVERLAY_BUTTON_EXTERNAL_DOWNLOADER,
                view -> VideoUtils.launchVideoExternalDownloader(),
                view -> {
                    VideoUtils.launchLongPressVideoExternalDownloader();
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
                instance = new ExternalDownload(viewGroup);
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