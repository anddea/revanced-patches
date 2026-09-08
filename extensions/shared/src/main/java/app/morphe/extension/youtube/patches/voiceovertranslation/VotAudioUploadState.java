/* Copyright (C) 2026 COOLak. Licensed under GPL-3.0-only. */

package app.morphe.extension.youtube.patches.voiceovertranslation;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BooleanSupplier;

/** Owned by one translation request and used only by that request's worker. */
final class VotAudioUploadState {
    private final Set<String> attemptedUploads = new HashSet<>();
    private final Set<String> failedUrls = new HashSet<>();

    void handle(String url, String translationId, BooleanSupplier upload,
                Runnable reportFailure, Runnable sendEmptyAudio) {
        boolean hasId = translationId != null && !translationId.isEmpty();
        if (hasId) {
            // Yandex may repeat AUDIO_REQUESTED while an accepted upload is processing.
            // Repetition must never turn a successful upload into a failure notification.
            if (!attemptedUploads.add(url + "#" + translationId)) return;
            if (upload.getAsBoolean()) return;
        }
        if (failedUrls.add(url)) reportFailure.run();
        if (hasId) sendEmptyAudio.run();
    }
}
