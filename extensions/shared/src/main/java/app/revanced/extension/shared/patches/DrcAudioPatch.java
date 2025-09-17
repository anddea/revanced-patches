package app.revanced.extension.shared.patches;

import app.revanced.extension.shared.settings.BaseSettings;

@SuppressWarnings("unused")
public class DrcAudioPatch {
    private static final boolean DISABLE_DRC_AUDIO =
            BaseSettings.DISABLE_DRC_AUDIO.get();

    public static boolean disableDrcAudio() {
        return DISABLE_DRC_AUDIO;
    }

    public static boolean disableDrcAudioFeatureFlag(boolean original) {
        return !DISABLE_DRC_AUDIO && original;
    }
}
