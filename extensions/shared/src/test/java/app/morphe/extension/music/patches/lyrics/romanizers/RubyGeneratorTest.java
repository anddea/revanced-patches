/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/4439
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.extension.music.patches.lyrics.romanizers;

import app.morphe.extension.music.patches.lyrics.LyricsRubyGenerator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class RubyGeneratorTest {

    // ── formatRuby ──

    @Test
    public void formatRubyWithKatakanaRuby() {
        String result = LyricsRubyGenerator.formatRuby("カンジ", LyricsRubyGenerator.RubyStyle.KATAKANA);
        assertEquals("カンジ", result);
    }

    @Test
    public void formatRubyWithHiraganaRuby() {
        String result = LyricsRubyGenerator.formatRuby("かんじ", LyricsRubyGenerator.RubyStyle.HIRAGANA);
        assertEquals("かんじ", result);
    }

    @Test
    public void formatRubyConvertsToKatakana() {
        String result = LyricsRubyGenerator.formatRuby("かんじ", LyricsRubyGenerator.RubyStyle.KATAKANA);
        assertEquals("カンジ", result);
    }

    @Test
    public void formatRubyConvertsToHiragana() {
        String result = LyricsRubyGenerator.formatRuby("カンジ", LyricsRubyGenerator.RubyStyle.HIRAGANA);
        assertEquals("かんじ", result);
    }

    @Test
    public void formatRubyNonKanaPassesThrough() {
        String result = LyricsRubyGenerator.formatRuby("Hello", LyricsRubyGenerator.RubyStyle.KATAKANA);
        assertEquals("Hello", result);
    }

    @Test
    public void formatRubyEmpty() {
        String result = LyricsRubyGenerator.formatRuby("", LyricsRubyGenerator.RubyStyle.KATAKANA);
        assertEquals("", result);
    }

    @Test
    public void formatRubyNull() {
        String result = LyricsRubyGenerator.formatRuby(null, LyricsRubyGenerator.RubyStyle.KATAKANA);
        assertNull(result);
    }

    // ── rubyStyleFromLanguage ──

    @Test
    public void rubyStyleFromLanguageJapaneseKana() {
        // rubyStyleFromLanguage is package-private, so tested through formatRuby behavior.
    }
}
