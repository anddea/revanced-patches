package app.morphe.extension.music.settings;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ActivityHookTest {
    @Test
    public void rootSettingsIntentOpensSearchableSettingsHost() {
        assertTrue(ActivityHook.isSearchableSettingsIntent("revanced_settings_intent"));
    }

    @Test
    public void finishIsNotInterceptedWithoutActiveSearch() {
        assertFalse(ActivityHook.handleFinish());
    }
}
