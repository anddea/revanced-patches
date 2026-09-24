/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
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

/** Per-video ownership of the initial translation pause. No Android dependencies. */
final class TranslationPlaybackState {
    static final int NONE = 0, YANDEX = 1, GOOGLE = 2;
    private String videoId = "";
    private int provider;
    private boolean waiting;
    private boolean requested;
    private boolean resume;
    private boolean awaitingVideo;
    private int pendingProvider;
    private boolean pendingPause;
    private Boolean pendingPlay;
    private boolean deferredResume;

    /** A new native player may still belong to the same logical video. */
    synchronized void initialize(int provider, boolean pause) {
        awaitingVideo = true;
        // Until the video ID is known, a manually requested translation still owns its hold.
        pendingProvider = pause || !waiting ? provider : this.provider;
        pendingPause = (pause || waiting) && pendingProvider != NONE;
        pendingPlay = null;
    }

    synchronized boolean newVideo(String id, int provider, boolean pause) {
        if (id.isEmpty()) return false;
        if (id.equals(videoId)) {
            if (awaitingVideo && pendingPlay != null) {
                if (waiting) resume = pendingPlay;
                else deferredResume = pendingPlay;
            }
            awaitingVideo = false;
            return false;
        }
        this.provider = provider;
        waiting = pause && provider != NONE;
        requested = false;
        resume = waiting && (!awaitingVideo || pendingPlay == null || pendingPlay);
        // A provisional hold for the old video may have intercepted this video's autoplay.
        deferredResume = awaitingVideo && !waiting && Boolean.TRUE.equals(pendingPlay);
        awaitingVideo = false;
        videoId = id;
        return true;
    }

    /** Only the confirmed replacement player may consume a deferred play request. */
    synchronized boolean takeDeferredResume() {
        if (awaitingVideo || waiting) return false;
        boolean result = deferredResume;
        deferredResume = false;
        return result;
    }

    synchronized void select(String id, int provider, boolean pause, boolean playing) {
        configure(id, provider, pause, playing);
        requested = true;
    }

    synchronized void configure(String id, int provider, boolean pause, boolean playing) {
        boolean alreadyHeld = waiting && id.equals(videoId);
        deferredResume = false;
        videoId = id;
        this.provider = provider;
        requested = false;
        waiting = pause;
        resume = pause && (alreadyHeld ? resume : playing);
    }

    synchronized boolean filterPlay(boolean playing, boolean internalChange) {
        if (awaitingVideo) {
            if (!pendingPause) return playing;
            if (!internalChange) pendingPlay = playing;
            return false;
        }
        if (!waiting) return playing;
        if (playing) {
            resume = true;
            return false;
        }
        if (!internalChange) resume = false;
        return false;
    }

    synchronized boolean isWaiting(int provider, String id) {
        return waiting && this.provider == provider && videoId.equals(id);
    }

    synchronized boolean matchesVideo(String id) { return !awaitingVideo && !id.isEmpty() && videoId.equals(id); }
    synchronized boolean isWaiting() { return awaitingVideo ? pendingPause : waiting; }
    synchronized int provider() { return awaitingVideo ? pendingProvider : provider; }
    synchronized String videoId() { return videoId; }
    synchronized boolean hasRequest() { return requested; }

    synchronized boolean ready(int provider, String id) {
        if (!isWaiting(provider, id)) return false;
        boolean shouldResume = resume;
        waiting = false;
        resume = false;
        if (awaitingVideo) {
            deferredResume = shouldResume;
            return false;
        }
        return shouldResume;
    }

    synchronized void cancel(int provider, String id) {
        if (this.provider == provider && videoId.equals(id)) {
            waiting = false;
            resume = false;
            deferredResume = false;
        }
    }
}
