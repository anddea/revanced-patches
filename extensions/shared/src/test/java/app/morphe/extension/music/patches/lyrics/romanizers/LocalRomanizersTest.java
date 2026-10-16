package app.morphe.extension.music.patches.lyrics.romanizers;

import static org.junit.Assert.assertEquals;

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
}
