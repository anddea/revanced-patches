/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
 * - Jav1x (https://github.com/Jav1x)
 *
 * Licensed under the GNU General Public License v3.0.
 *
 * ------------------------------------------------------------------------
 * GPLv3 Section 7 – Additional Terms & Attribution Requirements
 * ------------------------------------------------------------------------
 *
 * This file contains substantial original work by the author(s) listed above.
 *
 * In accordance with Section 7 of the GNU General Public License v3.0,
 * the following additional terms apply to this file:
 *
 * 1. Source Credit Preservation (Section 7(b)): This specific copyright notice
 *    and the list of original authors above must be preserved in any copy
 *    or derivative work. You may add your own copyright notice below it,
 *    but you may not remove the original one.
 *
 * 2. Origin & Modification Marking (Section 7(c)): Modified versions must be
 *    clearly marked as such (e.g., by adding a "Modified by" line or a new
 *    copyright notice) and must not be misrepresented as the original work.
 *
 * 3. Version Control Attribution (Section 7(b)): Any ports or substantial
 *    modifications must retain historical authorship credit in version control
 *    systems (e.g., Git), listing original author(s) appropriately and
 *    modifiers as committers or co-authors.
 *
 * 4. User Interface Attribution (Section 7(b)): Any works containing or
 *    derived from this material must maintain a visible credit or
 *    acknowledgment to the original author(s) within the application's
 *    user interface (e.g., in an "About" or "Credits" section).
 */

package app.morphe.extension.youtube.patches.voiceovertranslation;

import android.media.AudioTrack;

import java.lang.ref.WeakReference;

import app.morphe.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public final class VotOriginalVolumePatch {
    private static volatile WeakReference<AudioTrack> lastAudioTrackRef = new WeakReference<>(null);
    private static volatile float lastBaseVolume = 1.0f;

    private static float applyMultiplier(float volume) {
        if (!VoiceOverTranslationPatch.isTranslationActive()) {
            return volume;
        }
        int percent = Settings.VOT_ORIGINAL_AUDIO_VOLUME.get();
        float mult = percent / 100.0f;
        float result = volume * mult;
        if (Float.isNaN(result) || result < 0f) return 0f;
        return Math.min(result, 1f);
    }

    /**
     * Applies the VOT original volume multiplier to the given volume.
     * Called from bytecode patch before AudioTrack.setVolume(F).
     * Only when translation is actively playing, dims original audio so translation is audible.
     * Clamps result to 0..1 and handles NaN.
     *
     * @param audioTrack current player audio track receiving setVolume
     * @param volume original volume (0..1) from ExoPlayer
     * @return volume * (VOT_ORIGINAL_AUDIO_VOLUME/100) when translation playing, else unchanged
     */
    public static float applyVolumeMultiplier(AudioTrack audioTrack, float volume) {
        if (audioTrack != null) {
            lastAudioTrackRef = new WeakReference<>(audioTrack);
        }
        if (!Float.isNaN(volume)) {
            if (volume < 0f) {
                lastBaseVolume = 0f;
            } else lastBaseVolume = Math.min(volume, 1f);
        }
        return applyMultiplier(volume);
    }

    /**
     * Applies current VOT original-audio setting immediately to the last known AudioTrack.
     *
     * @return true if update was applied
     */
    public static boolean applyCurrentMultiplierNow() {
        AudioTrack audioTrack = lastAudioTrackRef.get();
        if (audioTrack == null) return false;
        float base = lastBaseVolume;
        if (Float.isNaN(base)) base = 1.0f;
        if (base < 0f) base = 0f;
        if (base > 1f) base = 1f;
        float adjusted = applyMultiplier(base);
        try {
            audioTrack.setVolume(adjusted);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
