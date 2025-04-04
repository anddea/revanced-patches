package app.revanced.extension.youtube.patches.video;

import app.revanced.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public class AV1CodecPatch {
    private static final String VP9_CODEC = "video/x-vnd.on2.vp9";

    /**
     * Replace the SW AV01 codec to VP9 codec.
     * May not be valid on some clients.
     *
     * @param original hardcoded value - "video/av01"
     */
    public static String replaceCodec(String original) {
        return Settings.REPLACE_AV1_CODEC.get() ? VP9_CODEC : original;
    }
}
