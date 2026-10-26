package app.morphe.extension.music.patches.lyrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

/** Provider ordering must preserve explicit choices and avoid enabling new word-list downloads. */
public class AiProvidersTest {
    @Test public void defaultsKeepExternalAiAndDictionaryProvidersOff() {
        assertEquals(List.of(AiProviders.EMBEDDED, AiProviders.GOOGLE),
                AiProviders.enabledProviders(AiProviders.DEFAULT_TRANSLATION_ORDER,
                        AiProviders.TRANSLATION_ORDER));
        assertEquals(List.of(AiProviders.EMBEDDED, AiProviders.GOOGLE),
                AiProviders.enabledProviders(AiProviders.DEFAULT_ROMANIZATION_ORDER,
                        AiProviders.ROMANIZATION_ORDER));
    }

    @Test public void explicitPrioritiesAndDisabledEntriesSurviveRoundTrip() {
        String order = "-embedded,local,google,-rime,-openai_compatible";
        var parsed = AiProviders.parseOrder(order, AiProviders.ROMANIZATION_ORDER);
        assertEquals(order, AiProviders.serializeOrder(parsed));
        assertEquals(List.of(AiProviders.LOCAL, AiProviders.GOOGLE),
                AiProviders.enabledProviders(order, AiProviders.ROMANIZATION_ORDER));
    }

    @Test public void unknownAndDuplicateEntriesDoNotCreateProviders() {
        var parsed = AiProviders.parseOrder("google,unknown,-google", AiProviders.TRANSLATION_ORDER);
        assertEquals(AiProviders.TRANSLATION_ORDER.size(), parsed.size());
        assertEquals(new AiProviders.ProviderEntry(AiProviders.GOOGLE, true), parsed.get(1));
    }

    @Test public void upgradingAnOlderListDoesNotEnableRimeDownloads() {
        var parsed = AiProviders.parseOrder("embedded,google,local,openai_compatible",
                AiProviders.ROMANIZATION_ORDER);
        assertFalse(parsed.stream().filter(p -> AiProviders.RIME.equals(p.id()))
                .findFirst().orElseThrow().enabled());
    }

    @Test public void changingOneProviderKeepsOtherChoicesAndPriorities() {
        String updated = AiProviders.withProviderEnabled(AiProviders.DEFAULT_ROMANIZATION_ORDER,
                AiProviders.ROMANIZATION_ORDER, AiProviders.LOCAL, true);
        assertEquals("embedded,google,local,-rime,-openai_compatible", updated);
        assertTrue(AiProviders.enabledProviders(updated, AiProviders.ROMANIZATION_ORDER)
                .contains(AiProviders.LOCAL));
    }
}
