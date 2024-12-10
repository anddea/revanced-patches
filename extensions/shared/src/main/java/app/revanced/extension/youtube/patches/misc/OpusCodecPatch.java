package app.revanced.extension.youtube.patches.misc;

import app.revanced.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public class OpusCodecPatch {

    public static boolean enableOpusCodec() {
        return Settings.ENABLE_OPUS_CODEC.get();
    }
}
