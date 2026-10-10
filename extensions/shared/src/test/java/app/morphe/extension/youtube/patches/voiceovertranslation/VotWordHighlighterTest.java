/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to this code.
 */

package app.morphe.extension.youtube.patches.voiceovertranslation;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class VotWordHighlighterTest {

    private static TranscriptSegment seg(long startMs, long endMs, String text) {
        return new TranscriptSegment(startMs, endMs, text, "ru-RU");
    }

    @Test
    public void splitWordsDropsBlanks() {
        assertEquals(Arrays.asList("ab", "c"), VotWordHighlighter.splitWords("  ab   c  "));
        assertTrue(VotWordHighlighter.splitWords("   ").isEmpty());
        assertTrue(VotWordHighlighter.splitWords(null).isEmpty());
    }

    @Test
    public void timingsAreProportionalToWordLength() {
        List<String> words = Arrays.asList("ab", "cdef");
        long[] starts = VotWordHighlighter.allocateWordStartTimes(words, 1000, 600);
        assertArrayEquals(new long[]{1000, 1200}, starts);
    }

    @Test
    public void lastWordEndsExactlyAtWindowEnd() {
        List<String> words = Arrays.asList("a", "bb", "ccc");
        long[] starts = VotWordHighlighter.allocateWordStartTimes(words, 0, 100);
        // Weights 1,2,3 of 100ms -> boundaries at 100/6 and 300/6, last word fills the rest.
        assertArrayEquals(new long[]{0, 16, 50}, starts);
    }

    @Test
    public void zeroDurationPutsEveryWordAtStart() {
        long[] starts = VotWordHighlighter.allocateWordStartTimes(
                Arrays.asList("a", "b"), 500, 0);
        assertArrayEquals(new long[]{500, 500}, starts);
    }

    @Test
    public void passedWordsUseInclusiveStartBoundary() {
        long[] starts = {1000, 1200, 1500};
        assertEquals(0, VotWordHighlighter.passedWordCount(starts, 999));
        assertEquals(1, VotWordHighlighter.passedWordCount(starts, 1000));
        assertEquals(2, VotWordHighlighter.passedWordCount(starts, 1499));
        assertEquals(3, VotWordHighlighter.passedWordCount(starts, 10_000));
    }

    @Test
    public void activeSegmentUsesHalfOpenWindow() {
        List<TranscriptSegment> segments = Arrays.asList(
                seg(0, 2000, "one two"), seg(2000, 4000, "three four"));
        assertEquals(0, VotWordHighlighter.findActiveSegmentIndex(segments, 0));
        assertEquals(0, VotWordHighlighter.findActiveSegmentIndex(segments, 1999));
        assertEquals(1, VotWordHighlighter.findActiveSegmentIndex(segments, 2000));
        assertEquals(-1, VotWordHighlighter.findActiveSegmentIndex(segments, 4000));
    }

    @Test
    public void gapBetweenSegmentsHasNoActiveSegment() {
        List<TranscriptSegment> segments = Arrays.asList(
                seg(0, 1000, "one"), seg(2000, 3000, "two"));
        assertEquals(-1, VotWordHighlighter.findActiveSegmentIndex(segments, 1500));
    }

    @Test
    public void highlightSnapshotTracksPlayback() {
        List<TranscriptSegment> segments = Collections.singletonList(seg(0, 2000, "one two"));
        VotWordHighlighter.Highlight at = VotWordHighlighter.highlightAt(segments, 1500);
        assertNotNull(at);
        assertEquals(0, at.segmentIndex());
        assertEquals(2, at.words().size());
        assertEquals("one", at.words().get(0).text());
        // "one"/"two" split 2000ms evenly -> second word starts at 1000ms, both passed at 1500.
        assertEquals(2, at.passedWords());
        assertEquals(1, VotWordHighlighter.highlightAt(segments, 999).passedWords());
    }

    @Test
    public void highlightIsNullWhenNothingAudible() {
        List<TranscriptSegment> segments = Collections.singletonList(seg(0, 1000, "one"));
        assertNull(VotWordHighlighter.highlightAt(segments, 1000));
        assertNull(VotWordHighlighter.highlightAt(Collections.emptyList(), 0));
        assertNull(VotWordHighlighter.highlightAt(
                Collections.singletonList(seg(0, 1000, "   ")), 500));
    }

    @Test
    public void highlightFollowsAdjustedPlaybackWindow() {
        TranscriptSegment shifted = seg(0, 1000, "one two");
        shifted.playbackStartMs = 500;
        shifted.playbackEndMs = 2500;
        VotWordHighlighter.Highlight at = VotWordHighlighter.highlightAt(
                Collections.singletonList(shifted), 499);
        assertNull(at);
        at = VotWordHighlighter.highlightAt(Collections.singletonList(shifted), 1500);
        assertNotNull(at);
        assertEquals(500, at.words().get(0).startMs());
        assertEquals(1500, at.words().get(1).startMs());
    }

    @Test
    public void realTokenTimingsWinOverInterpolation() {
        TranscriptSegment line = seg(0, 2000, "one two");
        line.timedWords = Arrays.asList(
                new VotWordHighlighter.Word("one", 100),
                new VotWordHighlighter.Word("two", 300));
        List<TranscriptSegment> segments = Collections.singletonList(line);

        VotWordHighlighter.Highlight at = VotWordHighlighter.highlightAt(segments, 250);
        assertNotNull(at);
        assertEquals("one", at.words().get(0).text());
        assertEquals("two", at.words().get(1).text());
        assertEquals(100, at.words().get(0).startMs());
        assertEquals(1, at.passedWords());

        assertEquals(2, VotWordHighlighter.highlightAt(segments, 300).passedWords());
    }

    @Test
    public void emptyTimedWordsFallBackToInterpolation() {
        TranscriptSegment line = seg(0, 2000, "one two");
        line.timedWords = Collections.emptyList();
        VotWordHighlighter.Highlight at = VotWordHighlighter.highlightAt(
                Collections.singletonList(line), 1500);
        assertNotNull(at);
        assertEquals(1000, at.words().get(1).startMs());
    }

    @Test
    public void renderKeyChangesOnlyOnVisibleChange() {
        List<VotWordHighlighter.Word> words = Arrays.asList(
                new VotWordHighlighter.Word("one", 0),
                new VotWordHighlighter.Word("two", 1000));
        VotWordHighlighter.Highlight a = new VotWordHighlighter.Highlight(0, words, 1);
        VotWordHighlighter.Highlight same = new VotWordHighlighter.Highlight(0, words, 1);
        assertEquals(VotWordHighlighter.renderKey(a, "v"), VotWordHighlighter.renderKey(same, "v"));
        assertNotEquals(VotWordHighlighter.renderKey(a, "v"),
                VotWordHighlighter.renderKey(new VotWordHighlighter.Highlight(0, words, 2), "v"));
        assertNotEquals(VotWordHighlighter.renderKey(a, "v"), VotWordHighlighter.renderKey(a, "w"));
        assertNotEquals(VotWordHighlighter.renderKey(a, "v"),
                VotWordHighlighter.renderKey(new VotWordHighlighter.Highlight(1, words, 1), "v"));
    }

    @Test
    public void plainTextJoinsWordsWithSpaces() {
        VotWordHighlighter.Highlight highlight = new VotWordHighlighter.Highlight(0, Arrays.asList(
                new VotWordHighlighter.Word("one", 0),
                new VotWordHighlighter.Word("two", 1000)), 1);
        assertEquals("one two", VotWordHighlighter.plainText(highlight));
    }

    @Test
    public void extrapolationFollowsPlayback() {
        assertEquals(1500, VotWordHighlighter.extrapolateVideoTime(1000, 5000, 5500, 1f, true));
        assertEquals(2000, VotWordHighlighter.extrapolateVideoTime(1000, 5000, 5500, 2f, true));
        assertEquals(1500, VotWordHighlighter.extrapolateVideoTime(1000, 5000, 5500, 0f, true));
    }

    @Test
    public void extrapolationStallsWhenNotPlaying() {
        assertEquals(1000, VotWordHighlighter.extrapolateVideoTime(1000, 5000, 9000, 1f, false));
        assertEquals(-1, VotWordHighlighter.extrapolateVideoTime(-1, 5000, 9000, 1f, true));
        assertEquals(1000, VotWordHighlighter.extrapolateVideoTime(1000, 5000, 5000, 1f, true));
    }

    @Test
    public void extrapolationIsCappedPastStalledHook() {
        assertEquals(3000, VotWordHighlighter.extrapolateVideoTime(1000, 0, 60_000, 1f, true));
    }

    private static VotWordHighlighter.Highlight highlightAt(int passed) {
        return new VotWordHighlighter.Highlight(0, Arrays.asList(
                new VotWordHighlighter.Word("one", 0),
                new VotWordHighlighter.Word("two", 1000),
                new VotWordHighlighter.Word("three", 2000)), passed);
    }

    @Test
    public void trackerPassesProgressThrough() {
        VotHighlightTracker tracker = new VotHighlightTracker();
        assertEquals(1, tracker.next("v", highlightAt(1), 500).passedWords());
        assertEquals(2, tracker.next("v", highlightAt(2), 1500).passedWords());
    }

    @Test
    public void trackerClampsPauseJitter() {
        VotHighlightTracker tracker = new VotHighlightTracker();
        assertEquals(2, tracker.next("v", highlightAt(2), 1500).passedWords());
        // Clock snapped slightly back (pause): already-spoken words stay painted.
        assertEquals(2, tracker.next("v", highlightAt(1), 1200).passedWords());
    }

    @Test
    public void trackerHonorsRealSeekBack() {
        VotHighlightTracker tracker = new VotHighlightTracker();
        assertEquals(3, tracker.next("v", highlightAt(3), 5000).passedWords());
        // Large backward jump is a seek, not jitter: highlight follows back.
        assertEquals(1, tracker.next("v", highlightAt(1), 500).passedWords());
    }

    @Test
    public void trackerResetsOnNewSegmentVideoOrGap() {
        VotHighlightTracker tracker = new VotHighlightTracker();
        assertEquals(2, tracker.next("v", highlightAt(2), 1500).passedWords());
        assertNull(tracker.next("v", null, 1600));
        // After a gap the next highlight starts fresh, even if smaller.
        assertEquals(1, tracker.next("v", highlightAt(1), 1700).passedWords());
        // New video also resets.
        assertEquals(3, tracker.next("v", highlightAt(3), 5000).passedWords());
        assertEquals(1, tracker.next("w", highlightAt(1), 5100).passedWords());
    }
}
