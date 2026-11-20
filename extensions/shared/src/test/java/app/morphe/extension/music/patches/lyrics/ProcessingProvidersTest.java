/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/4439
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.extension.music.patches.lyrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

public class ProcessingProvidersTest {

    private static final List<String> KNOWN_TRANSLATION =
            List.of(AiProviders.EMBEDDED, AiProviders.GOOGLE,
                    AiProviders.OPENAI_COMPATIBLE, AiProviders.DEEPL);

    private static final List<String> KNOWN_ROMANIZATION =
            List.of(AiProviders.EMBEDDED, AiProviders.GOOGLE,
                    AiProviders.LOCAL, AiProviders.RIME,
                    AiProviders.ICU, AiProviders.OPENAI_COMPATIBLE);

    // ── parseOrder ──

    @Test
    public void parseOrderNullUsesDefaultsAllEnabled() {
        List<AiProviders.ProviderEntry> entries =
                AiProviders.parseOrder(null, KNOWN_TRANSLATION);
        assertEquals(KNOWN_TRANSLATION.size(), entries.size());
        for (AiProviders.ProviderEntry entry : entries) {
            assertTrue(entry.enabled());
        }
    }

    @Test
    public void parseOrderEmptyUsesDefaults() {
        List<AiProviders.ProviderEntry> entries =
                AiProviders.parseOrder("", KNOWN_TRANSLATION);
        assertEquals(KNOWN_TRANSLATION.size(), entries.size());
    }

    @Test
    public void parseOrderDisabledPrefix() {
        List<AiProviders.ProviderEntry> entries =
                AiProviders.parseOrder("embedded,-google", KNOWN_TRANSLATION);
        assertEquals(KNOWN_TRANSLATION.size(), entries.size());
        // google should be disabled
        for (AiProviders.ProviderEntry entry : entries) {
            if (entry.id().equals("google")) {
                assertFalse(entry.enabled());
            }
        }
    }

    @Test
    public void parseOrderEmbeddedStaysFirst() {
        List<AiProviders.ProviderEntry> entries =
                AiProviders.parseOrder("google,deepl", KNOWN_TRANSLATION);
        assertEquals(AiProviders.EMBEDDED, entries.get(0).id());
        assertTrue(entries.get(0).enabled());
    }

    @Test
    public void parseOrderDeduplicates() {
        List<AiProviders.ProviderEntry> entries =
                AiProviders.parseOrder("embedded,embedded", KNOWN_TRANSLATION);
        // embedded appears once in the stored list, then the rest are appended.
        long embeddedCount = entries.stream()
                .filter(e -> e.id().equals(AiProviders.EMBEDDED)).count();
        assertEquals(1, embeddedCount);
    }

    @Test
    public void parseOrderUnknownTokenIgnored() {
        List<AiProviders.ProviderEntry> entries =
                AiProviders.parseOrder("embedded,unknown_provider", KNOWN_TRANSLATION);
        assertEquals(KNOWN_TRANSLATION.size(), entries.size());
    }

    @Test
    public void parseOrderRimeOffForOldLists() {
        // A stored list that predates rime should have rime off.
        List<AiProviders.ProviderEntry> entries =
                AiProviders.parseOrder("embedded,google", KNOWN_ROMANIZATION);
        for (AiProviders.ProviderEntry entry : entries) {
            if (entry.id().equals(AiProviders.RIME)) {
                assertFalse(entry.enabled());
            }
        }
    }

    // ── enabledProviders ──

    @Test
    public void enabledProvidersNullAllEnabled() {
        List<String> enabled = AiProviders.enabledProviders(null, KNOWN_TRANSLATION);
        assertEquals(KNOWN_TRANSLATION.size(), enabled.size());
    }

    @Test
    public void enabledProvidersSkipsDisabled() {
        List<String> enabled = AiProviders.enabledProviders("embedded,-google",
                KNOWN_TRANSLATION);
        assertFalse(enabled.contains("google"));
        assertTrue(enabled.contains("embedded"));
    }

    // ── serializeOrder ──

    @Test
    public void serializeOrderRoundTrip() {
        List<AiProviders.ProviderEntry> entries =
                AiProviders.parseOrder(null, KNOWN_TRANSLATION);
        String serialized = AiProviders.serializeOrder(entries);
        List<AiProviders.ProviderEntry> parsed =
                AiProviders.parseOrder(serialized, KNOWN_TRANSLATION);
        assertEquals(entries.size(), parsed.size());
        for (int i = 0; i < entries.size(); i++) {
            assertEquals(entries.get(i).id(), parsed.get(i).id());
            assertEquals(entries.get(i).enabled(), parsed.get(i).enabled());
        }
    }

    @Test
    public void serializeOrderEmptyList() {
        assertEquals("", AiProviders.serializeOrder(List.of()));
    }

    // ── withProviderEnabled ──

    @Test
    public void withProviderEnabledEnables() {
        String result = AiProviders.withProviderEnabled(
                "embedded,-google", KNOWN_TRANSLATION, "google", true);
        assertTrue(AiProviders.parseOrder(result, KNOWN_TRANSLATION)
                .stream().anyMatch(e -> e.id().equals("google") && e.enabled()));
    }

    @Test
    public void withProviderEnabledDisables() {
        String result = AiProviders.withProviderEnabled(
                "embedded,google", KNOWN_TRANSLATION, "google", false);
        assertTrue(AiProviders.parseOrder(result, KNOWN_TRANSLATION)
                .stream().anyMatch(e -> e.id().equals("google") && !e.enabled()));
    }

    @Test
    public void withProviderEnabledNoChangeReturnsSame() {
        String stored = "embedded,-google";
        String result = AiProviders.withProviderEnabled(
                stored, KNOWN_TRANSLATION, "google", false);
        assertEquals(stored, result);
    }

    // ── isAi ──

    @Test
    public void isAiTrueForOpenAiCompatible() {
        assertTrue(AiProviders.isAi(AiProviders.OPENAI_COMPATIBLE));
    }

    @Test
    public void isAiFalseForOtherProviders() {
        assertFalse(AiProviders.isAi(AiProviders.GOOGLE));
        assertFalse(AiProviders.isAi(AiProviders.DEEPL));
        assertFalse(AiProviders.isAi(AiProviders.EMBEDDED));
        assertFalse(AiProviders.isAi(null));
    }

    // ── constants ──

    @Test
    public void defaultTranslationOrder() {
        assertEquals("embedded,google,-openai_compatible,-deepl",
                AiProviders.DEFAULT_TRANSLATION_ORDER);
    }

    @Test
    public void defaultRomanizationOrder() {
        assertEquals("embedded,google,-local,-rime,icu,-openai_compatible",
                AiProviders.DEFAULT_ROMANIZATION_ORDER);
    }

    @Test
    public void endpointPresetsNotEmpty() {
        assertTrue(AiProviders.ENDPOINT_PRESETS.size() >= 10);
    }
}
