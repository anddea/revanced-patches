/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of https://github.com/anddea/revanced-patches/.
 *
 * The original author: https://github.com/Jav1x.
 *
 * IMPORTANT: This file is the proprietary work of https://github.com/Jav1x.
 * Any modifications, derivatives, or substantial rewrites of this file
 * must retain this copyright notice and the original author attribution
 * in the source code and version control history.
 */

package app.morphe.extension.youtube.patches.voiceovertranslation;

import app.morphe.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public final class VotOriginalVolumePatch {

    /**
     * Applies the VOT original volume multiplier to the given volume.
     * Called from bytecode patch before AudioTrack.setVolume(F).
     * Only when translation is actively playing, dims original audio so translation is audible.
     * Clamps result to 0..1 and handles NaN.
     *
     * @param volume original volume (0..1) from ExoPlayer
     * @return volume * (VOT_ORIGINAL_AUDIO_VOLUME/100) when translation playing, else unchanged
     */
    public static float applyVolumeMultiplier(float volume) {
        if (!VoiceOverTranslationPatch.isTranslationActive() && !VoiceOverTranslationPatch.translationStarting) return volume;
        int percent = Settings.VOT_ORIGINAL_AUDIO_VOLUME.get();
        float mult = percent / 100.0f;
        float result = volume * mult;
        if (Float.isNaN(result) || result < 0f) return 0f;
        if (result > 1f) return 1f;
        return result;
    }
}
