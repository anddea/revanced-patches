/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
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

package app.morphe.extension.youtube.patches.video;

import static app.morphe.extension.shared.utils.ResourceUtils.getString;
import static app.morphe.extension.shared.utils.StringRef.str;

import androidx.annotation.NonNull;

import java.util.Arrays;

import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.shared.VideoInformation;

/**
 * Parses the user-defined absolute audio-pitch presets used by the modern playback dialog.
 *
 * <p>An empty setting keeps the built-in relative pitch controls. Presets are entered as
 * whitespace-separated positive values, optionally with an {@code x} suffix, for example
 * {@code 0.85x 1.0x 1.3x}.</p>
 */
@SuppressWarnings("unused")
public final class CustomPlaybackAudioPitchPatch {
    /** The lowest audio-pitch value supported by the pitch adjustment controls. */
    public static final float PLAYBACK_AUDIO_PITCH_MINIMUM = 0.05f;

    /** The highest audio-pitch value supported by YouTube's playback parameters. */
    public static final float PLAYBACK_AUDIO_PITCH_MAXIMUM = VideoInformation.PLAYBACK_AUDIO_PITCH_MAXIMUM;

    private static final float[] EMPTY_PITCHES = new float[0];

    private static float[] customPlaybackAudioPitches = EMPTY_PITCHES;
    private static String loadedValue;

    static {
        loadCustomPitches();
    }

    /**
     * @return the sorted custom pitch presets, or an empty array when no custom presets are set.
     */
    public static synchronized float[] getPlaybackAudioPitches() {
        loadCustomPitches();
        return customPlaybackAudioPitches;
    }

    /**
     * Returns entries for the default audio-pitch preference. When no custom pitches are set,
     * the existing playback-speed entries are retained for backwards compatibility.
     */
    public static synchronized String[] getEntries() {
        final float[] pitches = getPlaybackAudioPitches();
        if (pitches.length == 0) {
            return CustomPlaybackSpeedPatch.getEntries();
        }

        final String[] entries = new String[pitches.length + 1];
        entries[0] = getString("quality_auto");
        for (int i = 0; i < pitches.length; i++) {
            entries[i + 1] = pitches[i] == 1.0f
                    ? getString("revanced_playback_speed_normal")
                    : pitches[i] + "x";
        }
        return entries;
    }

    /**
     * Returns values for the default audio-pitch preference.
     */
    public static synchronized String[] getEntryValues() {
        final float[] pitches = getPlaybackAudioPitches();
        if (pitches.length == 0) {
            return CustomPlaybackSpeedPatch.getEntryValues();
        }

        final String[] entryValues = new String[pitches.length + 1];
        entryValues[0] = "-2.0";
        for (int i = 0; i < pitches.length; i++) {
            entryValues[i + 1] = Float.toString(pitches[i]);
        }
        return entryValues;
    }

    /** Reloads the setting when it has changed since the last access. */
    public static synchronized void loadCustomPitches() {
        final String value = Settings.CUSTOM_PLAYBACK_AUDIO_PITCHES.get().trim();
        if (value.equals(loadedValue)) {
            return;
        }

        try {
            customPlaybackAudioPitches = parsePitches(value);
            loadedValue = value;
        } catch (PitchOutOfRangeException ex) {
            resetCustomPitches(str(
                    "revanced_custom_playback_audio_pitches_invalid",
                    PLAYBACK_AUDIO_PITCH_MINIMUM,
                    PLAYBACK_AUDIO_PITCH_MAXIMUM
            ));
            loadCustomPitches();
        } catch (Exception ex) {
            Logger.printInfo(() -> "Parse error", ex);
            resetCustomPitches(str("revanced_custom_playback_audio_pitches_parse_exception"));
            loadCustomPitches();
        }
    }

    private static float[] parsePitches(String value) {
        if (value.isEmpty()) {
            return EMPTY_PITCHES;
        }

        final String[] pitchStrings = value.split("\\s+");
        final float[] pitches = new float[pitchStrings.length];
        for (int i = 0; i < pitchStrings.length; i++) {
            String pitchString = pitchStrings[i];
            if (pitchString.endsWith("x") || pitchString.endsWith("X")) {
                pitchString = pitchString.substring(0, pitchString.length() - 1);
            }
            final float pitch = Float.parseFloat(pitchString);
            if (!Float.isFinite(pitch) || contains(pitches, i, pitch)) {
                throw new IllegalArgumentException();
            }
            if (pitch < PLAYBACK_AUDIO_PITCH_MINIMUM || pitch > PLAYBACK_AUDIO_PITCH_MAXIMUM) {
                throw new PitchOutOfRangeException();
            }
            pitches[i] = pitch;
        }

        Arrays.sort(pitches);
        return pitches;
    }

    private static boolean contains(float[] values, int length, float value) {
        for (int i = 0; i < length; i++) {
            if (Float.compare(values[i], value) == 0) {
                return true;
            }
        }
        return false;
    }

    private static void resetCustomPitches(@NonNull String toastMessage) {
        Utils.showToastLong(toastMessage);
        Utils.showToastShort(str("revanced_reset_to_default_toast"));
        Settings.CUSTOM_PLAYBACK_AUDIO_PITCHES.resetToDefault();
    }

    private static final class PitchOutOfRangeException extends IllegalArgumentException {
    }
}
