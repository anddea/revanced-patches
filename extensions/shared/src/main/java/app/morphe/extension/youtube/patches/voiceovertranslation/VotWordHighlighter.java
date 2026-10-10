/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Word-highlight model ported from ilyhalight/voice-over-translation
 * (src/subtitles/processor.ts allocateTimingsByLength, src/subtitles/activeCues.ts,
 * src/subtitles/widget.ts highlightWords). MIT, (c) sodapng / ilyhalight.
 *
 * <p>Two timing sources, same as the original: real per-word timings from the Yandex
 * subtitles API ({@code tokens} array, kept in {@link TranscriptSegment#timedWords})
 * and proportional interpolation for lines without them (e.g. translated Google captions).
 *
 * <p>Rendering lives in the Gemini subtitle overlay; this class only computes state.
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to this code.
 */

package app.morphe.extension.youtube.patches.voiceovertranslation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Word-level highlight state for VOT speech (Yandex tokens or interpolated timings).
 *
 * <p>The original plugin renders translated subtitles as word tokens with per-word
 * timings and paints every word the audio has already passed in a different color
 * ({@code --vot-subtitles-passed-color}). This class ports the timing half of that
 * logic: word splitting, proportional timing allocation, active-segment lookup and
 * passed-word counting. It is pure Java so it stays unit-testable on the JVM.
 *
 * <p>Rendering lives in the Gemini subtitle overlay; this class only computes state.
 */
public final class VotWordHighlighter {

    /** Maximum extrapolation past the last hook (stall guard for buffering). */
    static final long MAX_EXTRAPOLATION_AHEAD_MS = 2000L;

    private VotWordHighlighter() {
    }

    /** One word with its interpolated start time (video ms). */
    public record Word(String text, long startMs) {
    }

    /** Active segment plus per-word progress at a video timestamp. */
    public record Highlight(int segmentIndex, List<Word> words, int passedWords) {
    }

    /**
     * Splits segment text into spoken words. Whitespace-separated; empty parts dropped.
     * The original uses Intl.Segmenter with punctuation-aware slicing - this keeps the
     * coarser whitespace granularity so the port stays small.
     */
    public static List<String> splitWords(String text) {
        if (text == null) return Collections.emptyList();
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return Collections.emptyList();
        List<String> words = new ArrayList<>();
        for (String part : trimmed.split("\\s+")) {
            if (!part.isEmpty()) words.add(part);
        }
        return words;
    }

    /**
     * Ports {@code allocateTimingsByLength}: distributes {@code durationMs} across words
     * proportionally to their length (min weight 1). The last word always ends exactly
     * at {@code startMs + durationMs}; rounding drift stays inside earlier words.
     *
     * @return start time of each word, same size as {@code words}.
     */
    public static long[] allocateWordStartTimes(List<String> words, long startMs, long durationMs) {
        long[] starts = new long[words.size()];
        if (words.isEmpty()) return starts;
        long safeDuration = Math.max(0, durationMs);
        long totalWeight = 0;
        for (String word : words) totalWeight += Math.max(word.length(), 1);
        long prefix = 0;
        for (int i = 0; i < words.size(); i++) {
            starts[i] = startMs + safeDuration * prefix / totalWeight;
            prefix += Math.max(words.get(i).length(), 1);
        }
        return starts;
    }

    /**
     * Counts words the playback has already passed: every word with
     * {@code startMs <= timeMs}, mirroring the plugin's passed-word paint rule.
     */
    public static int passedWordCount(long[] wordStartMs, long timeMs) {
        int passed = 0;
        for (long start : wordStartMs) {
            if (start <= timeMs) passed++;
            else break;
        }
        return passed;
    }

    /**
     * Estimates the current playback position between video-time hook ticks.
     * The hook value lags behind reality (up to ~1s at 1x), which visibly delays word
     * highlighting behind the heard speech. Extrapolating from the last hook arrival
     * removes the inter-tick component of that lag.
     *
     * <p>All clock inputs are caller-provided (e.g. {@code SystemClock.elapsedRealtime()})
     * so this stays pure and unit-testable.
     *
     * @param hookMs last hooked video time, or negative if unknown.
     * @param hookElapsedMs monotonic clock at the moment {@code hookMs} was observed.
     * @param nowElapsedMs current monotonic clock value.
     * @param speed current playback speed (<= 0 treated as 1x).
     * @param playing false while paused/seeking/buffering - then the hook value is returned as-is.
     * @return estimated position, capped slightly above the hook so a stalled hook during
     *         buffering cannot run the highlight ahead of the speech.
     */
    public static long extrapolateVideoTime(long hookMs, long hookElapsedMs, long nowElapsedMs,
                                            float speed, boolean playing) {
        if (hookMs < 0 || !playing) return hookMs;
        long dt = nowElapsedMs - hookElapsedMs;
        if (dt <= 0) return hookMs;
        float rate = speed > 0f ? speed : 1f;
        long estimated = hookMs + (long) (dt * rate);
        long cap = hookMs + MAX_EXTRAPOLATION_AHEAD_MS;
        return Math.min(estimated, cap);
    }

    /**
     * Plain-text rendering of a highlight snapshot (words joined with spaces).
     * Used when word highlighting is disabled but subtitles are still shown.
     */
    public static String plainText(Highlight highlight) {
        StringBuilder sb = new StringBuilder();
        for (Word word : highlight.words()) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(word.text());
        }
        return sb.toString();
    }

    /**
     * Deduplication key for overlay rendering: same key means the visible text
     * (words and passed count) did not change and the view can be left alone.
     */
    public static String renderKey(Highlight highlight, String videoId) {
        return (videoId != null ? videoId : "") + '|' + highlight.segmentIndex()
                + '|' + highlight.passedWords() + '|' + highlight.words().hashCode();
    }

    /**
     * Ports the single-line case of {@code findActiveSubtitleLineIndices}: the segment
     * whose playback window contains {@code timeMs} ({@code start <= t < end}).
     *
     * @return segment index, or -1 when no segment is audible at {@code timeMs}.
     */
    public static int findActiveSegmentIndex(List<TranscriptSegment> segments, long timeMs) {
        for (int i = 0; i < segments.size(); i++) {
            TranscriptSegment seg = segments.get(i);
            if (seg.playbackStartMs <= timeMs && timeMs < seg.playbackEndMs) return i;
        }
        return -1;
    }

    /**
     * Builds the highlight snapshot for a video timestamp, or null when nothing is
     * audible (no active segment or empty text) so callers can hide their overlay.
     * Uses real Yandex token timings when the segment carries them, interpolates otherwise.
     */
    public static Highlight highlightAt(List<TranscriptSegment> segments, long timeMs) {
        if (segments == null || segments.isEmpty()) return null;
        int index = findActiveSegmentIndex(segments, timeMs);
        if (index < 0) return null;
        TranscriptSegment seg = segments.get(index);
        List<Word> timed = seg.timedWords;
        if (timed != null && !timed.isEmpty()) {
            return new Highlight(index, timed, passedWordCount(startTimesOf(timed), timeMs));
        }
        List<String> texts = splitWords(seg.text);
        if (texts.isEmpty()) return null;
        long[] starts = allocateWordStartTimes(texts, seg.playbackStartMs,
                seg.playbackEndMs - seg.playbackStartMs);
        List<Word> words = new ArrayList<>(texts.size());
        for (int i = 0; i < texts.size(); i++) words.add(new Word(texts.get(i), starts[i]));
        return new Highlight(index, Collections.unmodifiableList(words),
                passedWordCount(starts, timeMs));
    }

    private static long[] startTimesOf(List<Word> words) {
        long[] starts = new long[words.size()];
        for (int i = 0; i < words.size(); i++) starts[i] = words.get(i).startMs();
        return starts;
    }
}
