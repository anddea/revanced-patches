package app.revanced.extension.music.patches.misc;

import app.revanced.extension.music.settings.Settings;

@SuppressWarnings("unused")
public class DrcAudioPatch {
    private static final boolean DISABLE_DRC_AUDIO = Settings.DISABLE_DRC_AUDIO.get();

    public static float disableDrcAudio(float original) {
        if (DISABLE_DRC_AUDIO) {
            return 0f;
        }
        return original;
    }

    public static boolean disableDrcAudioFeatureFlag(boolean original) {
        return !DISABLE_DRC_AUDIO && original;
    }
}
