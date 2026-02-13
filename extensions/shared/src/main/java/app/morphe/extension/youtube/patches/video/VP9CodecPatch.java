package app.morphe.extension.youtube.patches.video;

import app.morphe.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public class VP9CodecPatch {

    public static boolean disableVP9Codec() {
        return !Settings.DISABLE_VP9_CODEC.get();
    }
}
