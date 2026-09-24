package app.morphe.extension.youtube.patches.voiceovertranslation;

import static org.junit.Assert.*;

import app.morphe.extension.youtube.settings.Settings;

import org.junit.Before;
import org.junit.Test;

public class PreferenceChangeMethodsTest {
    private PreferenceChangeMethods prefs;
    private final String service = Settings.GOOGLE_VOT_TRANSLATION_SERVICE.key;

    @Before
    public void before() throws Exception {
        new TranslationPlaybackControllerTest().before();
        prefs = new PreferenceChangeMethods();
        prefs.add(service, () -> {});
    }

    @Test
    public void serviceChangeReachesCurrentVideoWithoutRestart() {
        prefs.change(service);
        assertEquals(1, GoogleVoiceOverTranslationPatch.reloads);
        assertEquals(1, prefs.cacheSyncs);
        assertEquals(1, prefs.uiSyncs);
        assertFalse(prefs.updatingPreference);
    }

    @Test
    public void voiceChangeReachesCurrentVideo() {
        String key = Settings.GOOGLE_VOT_USE_NATIVE_TTS.key;
        prefs.add(key, () -> {});
        prefs.change(key);
        assertEquals(1, GoogleVoiceOverTranslationPatch.voiceChanges);
    }

    @Test
    public void listenerUsesTheFreshSettingCacheBeforeDispatch() {
        Settings.GOOGLE_VOT_ENABLED.value = true;
        String id = "same-video";
        app.morphe.extension.youtube.shared.VideoInformation.id = id;
        GoogleVoiceOverTranslationPatch.id = id;
        TranslationPlaybackController.newVideoLoaded(id);
        app.morphe.extension.shared.utils.Utils.drain();
        String key = Settings.GOOGLE_VOT_AUTO_TRANSLATE.key;
        prefs.add(key, () -> Settings.GOOGLE_VOT_AUTO_TRANSLATE.value = true);
        prefs.change(key);
        app.morphe.extension.shared.utils.Utils.drain();
        assertEquals(1, GoogleVoiceOverTranslationPatch.starts);
    }

    @Test
    public void importDoesNotStartTranslation() {
        prefs.settingImportInProgress = true;
        prefs.change(service);
        assertEquals(0, GoogleVoiceOverTranslationPatch.reloads);
        assertEquals(0, prefs.cacheSyncs);
        assertEquals(1, prefs.uiSyncs);
    }

    @Test
    public void recursiveSyncDoesNotDispatch() {
        prefs.updatingPreference = true;
        prefs.change(service);
        assertEquals(0, GoogleVoiceOverTranslationPatch.reloads);
    }

    @Test
    public void sliderDragDoesNotDispatch() {
        prefs.sliderInteractionInProgress = true;
        prefs.change(service);
        assertEquals(0, GoogleVoiceOverTranslationPatch.reloads);
    }

    @Test
    public void unknownSettingIsIgnored() {
        prefs.change("unknown");
        assertEquals(0, prefs.cacheSyncs);
    }

    @Test
    public void missingPreferenceIsIgnored() {
        prefs.preferences.clear();
        prefs.change(service);
        assertEquals(0, prefs.cacheSyncs);
    }

    @Test
    public void nullKeyIsIgnored() {
        prefs.change(null);
        assertEquals(0, prefs.cacheSyncs);
    }

    @Test
    public void userDialogDefersDispatch() {
        prefs.settings.get(service).userDialogMessage = "confirm";
        prefs.change(service);
        assertEquals(1, prefs.dialogs);
        assertEquals(0, GoogleVoiceOverTranslationPatch.reloads);
    }

    @Test
    public void restartPromptDoesNotPreventCurrentVideoUpdate() {
        prefs.settings.get(service).rebootApp = true;
        prefs.change(service);
        assertEquals(1, prefs.dialogs);
        assertEquals(1, GoogleVoiceOverTranslationPatch.reloads);
    }
}
