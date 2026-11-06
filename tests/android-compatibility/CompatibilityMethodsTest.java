import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.IOException;
import java.util.*;

public class CompatibilityMethodsTest {
    @Before public void reset() throws Exception {
        CompatibilityMethods.retryTimes.clear();
        CompatibilityMethods.Requester.connection = new CompatibilityMethods.Connection();
        CompatibilityMethods.Requester.title = "Тест 日本語 $1 \\";
        CompatibilityMethods.Requester.error = null;
    }
    private String render(String template, List<String> lines) {
        return CompatibilityMethods.renderPrompt(template, "translate", "Russian", "Title $1", "Artist\\", lines);
    }
    @Test public void promptReplacesAllVariablesAndPreservesLiteralReplacements() {
        assertEquals("before translate|Russian|Title $1|Artist\\|2|one\n$2\\\n after",
                render("before {task}|{language}|{title}|{artist}|{count}|{lines} after", List.of("one", "$2\\")));
    }
    @Test public void promptPreservesUnknownVariables() {
        assertEquals("{unknown} {task-name} {LANGUAGE} {1} {} translate",
                render("{unknown} {task-name} {LANGUAGE} {1} {} {task}", List.of()));
    }
    @Test public void promptPreservesUnmatchedText() {
        assertEquals("No variables here", render("No variables here", List.of()));
    }
    @Test public void promptHandlesRepeatedAdjacentVariables() {
        assertEquals("RussianRussiantranslate", render("{language}{language}{task}", List.of()));
    }
    @Test public void promptHandlesNullMetadataAndLines() {
        assertEquals("[] [] 2\n\nsecond\n", CompatibilityMethods.renderPrompt(
                "[{title}] [{artist}] {count}\n{lines}", "translate", "ru", null, null, Arrays.asList(null, "second")));
    }
    @Test public void promptHandlesEmptyLines() {
        assertEquals("0[]", render("{count}[{lines}]", List.of()));
    }
    @Test public void missingTaskAndLanguagePreserveTheirVariables() {
        assertEquals("{task} {language}", CompatibilityMethods.renderPrompt("{task} {language}", null, null, "", "", List.of()));
    }
    @Test public void nullAndEmptyTemplatesUseDefault() {
        String expected = CompatibilityMethods.DEFAULT_PROMPT
                .replace("{task}", "translate").replace("{language}", "Russian")
                .replace("{title}", "Title $1").replace("{artist}", "Artist\\")
                .replace("{count}", "1").replace("{lines}", "line\n");
        assertEquals(expected, render(null, List.of("line")));
        assertEquals(expected, render("", List.of("line")));
    }
    @Test public void legacyMusicVersionsDoNotCallUnsupportedLayoutApi() {
        for (int sdk : new int[] {26, 27, 28}) {
            CompatibilityMethods.Build.VERSION.SDK_INT = sdk;
            var methods = new CompatibilityMethods();
            methods.suppressLyricsLayout(true);
            methods.suppressLyricsLayout(false);
            assertTrue(methods.linesContainer.calls.isEmpty());
        }
    }
    @Test public void supportedLayoutApiKeepsBothEnableAndDisable() {
        for (int sdk : new int[] {29, 30, 36}) {
            CompatibilityMethods.Build.VERSION.SDK_INT = sdk;
            var methods = new CompatibilityMethods();
            methods.suppressLyricsLayout(true);
            methods.suppressLyricsLayout(false);
            assertEquals(List.of(true, false), methods.linesContainer.calls);
        }
    }
    @Test public void originalTitleEncodesTheNestedVideoUrl() {
        assertEquals("Тест 日本語 $1 \\", CompatibilityMethods.fetchTitle("Ab_12-xyz"));
        assertEquals("https://www.youtube.com/oembed?format=json&url=https%3A%2F%2Fwww.youtube.com%2Fwatch%3Fv%3DAb_12-xyz", CompatibilityMethods.Requester.url);
        assertEquals(5000, CompatibilityMethods.Requester.connection.getConnectTimeout());
        assertEquals(5000, CompatibilityMethods.Requester.connection.getReadTimeout());
        assertTrue(CompatibilityMethods.retryTimes.isEmpty());
    }
    @Test public void originalTitleEncodingUsesUtf8AndEscapesReservedCharacters() {
        CompatibilityMethods.fetchTitle("日 +&=/");
        assertTrue(CompatibilityMethods.Requester.url.endsWith("%E6%97%A5+%2B%26%3D%2F"));
    }
    @Test public void missingTitleReturnsNull() {
        CompatibilityMethods.Requester.title = "";
        assertNull(CompatibilityMethods.fetchTitle("a"));
    }
    @Test public void nonSuccessResponseDoesNotParseTitle() {
        for (int code : new int[] {199, 201, 403, 404, 500}) {
            CompatibilityMethods.Requester.connection.code = code;
            assertNull(CompatibilityMethods.fetchTitle("a"));
            assertTrue(CompatibilityMethods.retryTimes.isEmpty());
        }
    }
    @Test public void networkErrorSchedulesRetry() {
        CompatibilityMethods.Requester.error = new IOException("offline");
        assertNull(CompatibilityMethods.fetchTitle("a"));
        long retry = CompatibilityMethods.retryTimes.get("a");
        assertEquals(123_486_000L, retry);
    }
    @Test public void unexpectedErrorReturnsNullWithoutNetworkRetry() {
        CompatibilityMethods.Requester.error = new IllegalStateException("bad response");
        assertNull(CompatibilityMethods.fetchTitle("a"));
        assertTrue(CompatibilityMethods.retryTimes.isEmpty());
    }
}
