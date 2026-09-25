/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
 * - COOLak (https://github.com/COOLak)
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

import app.morphe.extension.youtube.settings.Settings;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BooleanSupplier;

/** Owned by one translation request and used only by that request's worker. */
public final class VotAudioUploadState {
    private final Set<String> attemptedUploads = new HashSet<>();
    private final Set<String> failedUrls = new HashSet<>();

    /** Reuses VOT's audio download and fallback policy for a subtitle-only request. */
    public void upload(String videoId, String url, String translationId) {
        handle(url, translationId,
                () -> VotAudioDownloader.downloadAndSend(videoId, url, translationId),
                () -> VotApiClient.sendFailedAudio(url),
                () -> VotApiClient.sendEmptyAudio(url, translationId,
                        Settings.VOT_USE_LIVE_VOICES.get()
                                ? Settings.VOT_OAUTH_TOKEN.get() : null));
    }

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
