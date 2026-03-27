package app.morphe.extension.shared.patches;

import app.morphe.extension.shared.settings.BaseSettings;

@SuppressWarnings("unused")
public class OpusCodecPatch {

    public static boolean enableOpusCodec() {
        return BaseSettings.ENABLE_OPUS_CODEC.get();
    }
}
