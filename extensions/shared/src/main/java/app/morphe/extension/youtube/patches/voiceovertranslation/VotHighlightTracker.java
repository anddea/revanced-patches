/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to this code.
 */

package app.morphe.extension.youtube.patches.voiceovertranslation;

/**
 * Keeps the passed-word count monotonic within one subtitle line.
 *
 * <p>The video clock jitters around pause/resume (small backward snaps), and a naive
 * highlight would unpaint already-spoken words on pause and bulk-repaint them on resume.
 * This tracker clamps small backward moves inside the same line while still honoring
 * real seeks (large backward jumps), line changes and video changes.
 */
public final class VotHighlightTracker {

    /** Backward jumps within this are treated as clock jitter, not seeks. */
    static final long UNPAINT_JITTER_MS = 2000L;

    private String lastSegKey;
    private int lastPassed;
    private long lastTimeMs;

    /**
     * @param videoId current video, scopes the state.
     * @param highlight freshly computed highlight, or null when nothing is audible.
     * @param nowMs video clock value {@code highlight} was computed from.
     * @return highlight to render (possibly with the passed count held back), or null.
     */
    public VotWordHighlighter.Highlight next(String videoId,
                                             VotWordHighlighter.Highlight highlight,
                                             long nowMs) {
        if (highlight == null) {
            lastSegKey = null;
            return null;
        }
        String segKey = videoId + '|' + highlight.segmentIndex();
        if (segKey.equals(lastSegKey)
                && highlight.passedWords() < lastPassed
                && lastTimeMs - nowMs <= UNPAINT_JITTER_MS) {
            return new VotWordHighlighter.Highlight(
                    highlight.segmentIndex(), highlight.words(), lastPassed);
        }
        lastSegKey = segKey;
        lastPassed = highlight.passedWords();
        lastTimeMs = nowMs;
        return highlight;
    }
}
