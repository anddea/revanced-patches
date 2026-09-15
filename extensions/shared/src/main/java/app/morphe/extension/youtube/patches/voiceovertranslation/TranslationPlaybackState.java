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
    private boolean resume;

    synchronized void reset(int provider, boolean pause) {
        videoId = "";
        this.provider = provider;
        waiting = pause && provider != NONE;
        resume = waiting;
    }

    synchronized boolean newVideo(String id, int provider, boolean pause) {
        if (id.isEmpty() || id.equals(videoId)) return false;
        // Preserve the early constructor pause when the metadata arrives.
        if (!videoId.isEmpty()) reset(provider, pause);
        else if (this.provider != provider || (!waiting && pause)) reset(provider, pause);
        videoId = id;
        return true;
    }

    synchronized void select(String id, int provider, boolean pause, boolean playing) {
        boolean alreadyHeld = waiting && id.equals(videoId);
        videoId = id;
        this.provider = provider;
        waiting = pause;
        resume = pause && (alreadyHeld ? resume : playing);
    }

    synchronized boolean filterPlay(boolean playing, boolean internalChange) {
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

    synchronized boolean matchesVideo(String id) { return !id.isEmpty() && videoId.equals(id); }
    synchronized boolean isWaiting() { return waiting; }
    synchronized int provider() { return provider; }
    synchronized String videoId() { return videoId; }

    synchronized boolean ready(int provider, String id) {
        if (!isWaiting(provider, id)) return false;
        boolean shouldResume = resume;
        waiting = false;
        resume = false;
        return shouldResume;
    }

    synchronized void cancel(int provider, String id) {
        if (this.provider == provider && videoId.equals(id)) {
            waiting = false;
            resume = false;
        }
    }
}
