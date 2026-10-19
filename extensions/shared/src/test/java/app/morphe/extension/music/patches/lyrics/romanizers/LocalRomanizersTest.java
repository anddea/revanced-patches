package app.morphe.extension.music.patches.lyrics.romanizers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;

import java.util.List;

import app.morphe.extension.music.patches.lyrics.Word;

import org.junit.Test;

/** Checks that local conversion stays script-specific and preserves unrelated text. */
public class LocalRomanizersTest {
    @Test
    public void kanaHandlesBothSyllabaries() {
        assertEquals("katakana", KanaRomanizer.romanize("カタカナ"));
        assertEquals("hiragana", KanaRomanizer.romanize("ひらがな"));
    }

    @Test
    public void kanaCombinesSmallKana() {
        assertEquals("kya", KanaRomanizer.romanize("きゃ"));
    }

    @Test
    public void hangulPreservesNonHangulText() {
        assertEquals("gana! ABC", HangulRomanizer.romanize("가나! ABC"));
    }

    @Test
    public void detectsScriptsWithoutConvertingLatin() {
        assertEquals(ScriptDetector.Script.OTHER, ScriptDetector.detect("Hello 123!"));
        assertEquals(ScriptDetector.Script.KOREAN, ScriptDetector.detect("한국어"));
        assertEquals(ScriptDetector.Script.JAPANESE, ScriptDetector.detect("ひらがな"));
        assertEquals(ScriptDetector.Script.HAN, ScriptDetector.detect("中文"));
    }
    @Test
    public void detectsAdditionalScripts() {
        assertEquals(ScriptDetector.Script.CYRILLIC, ScriptDetector.detect("Привет"));
        assertEquals(ScriptDetector.Script.GREEK, ScriptDetector.detect("Ελλάδα"));
        assertEquals(ScriptDetector.Script.GEORGIAN, ScriptDetector.detect("ქართული"));
        assertEquals(ScriptDetector.Script.ARMENIAN, ScriptDetector.detect("Հայերեն"));
    }

    @Test
    public void alphabetRomanizationPreservesUnrelatedText() {
        assertEquals("Privet ABC 123", CyrillicRomanizer.romanize("Привет ABC 123"));
        assertEquals("kalimera ABC", GreekRomanizer.romanize("καλημερα ABC"));
    }
    @Test
    public void alignsSpokenCharactersWithoutAssigningPunctuation() {
        int[][] spread = LocalRomanizer.spreadReading(List.of(
                new Word(0, 100, "你好"), new Word(100, 120, "！"),
                new Word(120, 200, "世界")), "ni hao shi jie");
        assertArrayEquals(new int[] {0, 2}, spread[0]);
        assertArrayEquals(new int[] {0, 0}, spread[1]);
        assertArrayEquals(new int[] {2, 4}, spread[2]);
    }

    @Test
    public void rejectsReadingsThatCannotFitTheTimedWords() {
        assertNull(LocalRomanizer.spreadReading(
                List.of(new Word(0, 100, "你好")), "ni hao extra"));
    }
}
