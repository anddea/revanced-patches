package app.morphe.extension.youtube.patches.video;

import android.view.Display.HdrCapabilities;

import app.morphe.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public class HDRVideoPatch {

    public static int[] disableHDRVideo(HdrCapabilities capabilities) {
        return Settings.DISABLE_HDR_VIDEO.get()
                ? new int[0]
                : capabilities.getSupportedHdrTypes();
    }
}
