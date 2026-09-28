/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.extension.youtube.patches.voiceovertranslation;

import android.media.AudioTrack;

import java.util.concurrent.atomic.AtomicReference;

import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;

/**
 * Multiplies the YouTube ExoPlayer audio sink volume by a specific multiplier for Google VOT.
 */
@SuppressWarnings("unused")
public final class GoogleVotOriginalVolumePatch {
    private static final long ENFORCE_INTERVAL_MS = 50;

    private static volatile float currentMultiplier = 1.0f;
    private static volatile float lastBaseVolume = 1.0f;
    private static volatile boolean enforceScheduled;
    private static final AtomicReference<AudioTrack> lastAudioTrackRef = new AtomicReference<>(null);

    private static float clamp01(float value) {
        if (Float.isNaN(value) || value < 0.0f) return 0.0f;
        return Math.min(value, 1.0f);
    }

    public static float getAudioMultiplier(float baseVolume) {
        if (!Float.isNaN(baseVolume)) lastBaseVolume = clamp01(baseVolume);
        return clamp01(baseVolume * currentMultiplier);
    }

    /**
     * Captures the active player {@link AudioTrack} and applies the current multiplier to it.
     */
    public static void setAudioTrack(AudioTrack track) {
        if (track == null) return;
        lastAudioTrackRef.set(track);
        applyToActiveTrack();
        if (currentMultiplier != 1.0f) scheduleEnforce();
    }

    /**
     * Sets the volume multiplier applied on top of YouTube's base volume.
     */
    public static void setAudioMultiplier(float multiplier) {
        float clamped = clamp01(multiplier);
        if (clamped == currentMultiplier) return;
        currentMultiplier = clamped;
        applyToActiveTrack();
        if (clamped != 1.0f) scheduleEnforce();
    }

    /**
     * Resets the multiplier to {@code 1.0f}.
     */
    public static void clearAudioMultiplier() {
        setAudioMultiplier(1.0f);
    }

    private static void scheduleEnforce() {
        if (enforceScheduled) return;
        enforceScheduled = true;
        Utils.runOnMainThreadDelayed(GoogleVotOriginalVolumePatch::enforceTick, ENFORCE_INTERVAL_MS);
    }

    private static void enforceTick() {
        enforceScheduled = false;
        // Stop the loop once ducking is off; setAudioMultiplier(<1.0) will restart it.
        if (currentMultiplier == 1.0f) return;
        applyToActiveTrack();
        scheduleEnforce();
    }

    private static void applyToActiveTrack() {
        AudioTrack track = lastAudioTrackRef.get();
        if (track == null) return;
        try {
            float target = clamp01(lastBaseVolume * currentMultiplier);
            track.setVolume(target);
        } catch (Exception ex) {
            Logger.printException(() -> "Failed to apply volume directly to AudioTrack", ex);
        }
    }
}
