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

import android.widget.Toast;

import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.shared.RootView;
import app.morphe.extension.shared.utils.Utils;

@SuppressWarnings("unused")
public final class VotOriginalVolumePatch {

    private static volatile boolean toastShown;

    /**
     * Applies the VOT original volume multiplier to the given volume.
     * Called from bytecode patch before AudioTrack.setVolume(F).
     * Clamps result to 0..1 and handles NaN.
     *
     * @param volume original volume (0..1) from ExoPlayer
     * @return volume * (VOT_ORIGINAL_AUDIO_VOLUME/100), clamped to 0..1
     */
    public static float applyVolumeMultiplier(float volume) {
        if (!toastShown) {
            toastShown = true;
            Utils.runOnMainThread(() -> {
                android.content.Context ctx = RootView.getContext();
                if (ctx != null) {
                    Toast.makeText(ctx, "VOT: original volume patch active", Toast.LENGTH_SHORT).show();
                }
            });
        }
        if (!Settings.VOT_ENABLED.get()) return volume;
        float mult = Settings.VOT_ORIGINAL_AUDIO_VOLUME.get() / 100.0f;
        float result = volume * mult;
        if (Float.isNaN(result) || result < 0f) return 0f;
        if (result > 1f) return 1f;
        return result;
    }
}
