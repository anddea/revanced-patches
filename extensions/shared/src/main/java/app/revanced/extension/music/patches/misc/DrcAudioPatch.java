package app.revanced.extension.music.patches.misc;

import app.revanced.extension.music.settings.Settings;

@SuppressWarnings("unused")
public class DrcAudioPatch {

    public static float disableDrcAudio(float original) {
        if (!Settings.DISABLE_DRC_AUDIO.get()) {
            return original;
        }
        return 0f;
    }
}
