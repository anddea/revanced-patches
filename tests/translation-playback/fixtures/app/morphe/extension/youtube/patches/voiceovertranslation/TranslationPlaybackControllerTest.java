package app.morphe.extension.youtube.patches.voiceovertranslation;

import static app.morphe.extension.youtube.patches.voiceovertranslation.TranslationPlaybackState.*;

import static org.junit.Assert.*;

import app.morphe.extension.shared.innertube.utils.PlayerResponseOuterClass.VideoDetails;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.shared.VideoInformation;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class TranslationPlaybackControllerTest {
    private Object director;
    private TranslationPlaybackState state;

    @Before
    public void before() throws Exception {
        for (Field f : Settings.class.getFields()) ((Settings.Flag) f.get(null)).value = false;
        Field f = TranslationPlaybackController.class.getDeclaredField("state");
        f.setAccessible(true);
        state = (TranslationPlaybackState) f.get(null);
        TranslationPlaybackState fresh = new TranslationPlaybackState();
        for (Field field : TranslationPlaybackState.class.getDeclaredFields())
            if (!Modifier.isStatic(field.getModifiers())) {
                field.setAccessible(true);
                field.set(state, field.get(fresh));
            }
        for (String name : new String[] {"automaticVideoId", "internalChange"}) {
            f = TranslationPlaybackController.class.getDeclaredField(name);
            f.setAccessible(true);
            f.set(null, name.equals("automaticVideoId") ? "" : false);
        }
        Utils.mainThread = true;
        Utils.queue.clear();
        VideoInformation.id = "";
        VideoInformation.player = new Object();
        VideoInformation.playing = false;
        VideoInformation.fail = false;
        VideoInformation.available = true;
        VideoInformation.playCalls = VideoInformation.pauseCalls = 0;
        VoiceOverTranslationPatch.pending = "";
        VoiceOverTranslationPatch.attempts =
                VoiceOverTranslationPatch.starts =
                        VoiceOverTranslationPatch.paused = VoiceOverTranslationPatch.suspended = 0;
        VoiceOverTranslationPatch.active = VoiceOverTranslationPatch.inflight = false;
        GoogleVoiceOverTranslationPatch.id = "";
        GoogleVoiceOverTranslationPatch.starts =
                GoogleVoiceOverTranslationPatch.paused =
                        GoogleVoiceOverTranslationPatch.suspended =
                                GoogleVoiceOverTranslationPatch.voiceChanges =
                                        GoogleVoiceOverTranslationPatch.reloads = 0;
        director = new Object();
    }

    private void yandex() {
        Settings.VOT_ENABLED.value = true;
        Settings.VOT_AUTO_TRANSLATE.value = true;
        Settings.VOT_PAUSE_VIDEO_WHILE_PREPARING_TRANSLATION.value = true;
    }

    private void google() {
        Settings.GOOGLE_VOT_ENABLED.value = true;
        Settings.GOOGLE_VOT_AUTO_TRANSLATE.value = true;
        Settings.GOOGLE_VOT_PAUSE_WHILE_PREPARING.value = true;
    }

    private void init() {
        TranslationPlaybackController.initialize(director);
    }

    private void load(String id) {
        VideoInformation.id = id;
        VoiceOverTranslationPatch.pending = id;
        TranslationPlaybackController.newVideoLoaded(id);
        Utils.drain();
    }

    private void ready(int provider) {
        if (provider == YANDEX) {
            VoiceOverTranslationPatch.inflight = false;
            VoiceOverTranslationPatch.active = true;
        }
        TranslationPlaybackController.ready(provider, VideoInformation.id);
        Utils.drain();
    }

    @Test
    public void automaticYandexStartsOnceAfterMetadata() {
        yandex();
        init();
        load("a");
        assertEquals(1, VoiceOverTranslationPatch.starts);
        assertFalse(VideoInformation.playing);
        TranslationPlaybackController.metadataLoaded("a");
        Utils.drain();
        assertEquals(1, VoiceOverTranslationPatch.starts);
        ready(YANDEX);
        assertTrue(VideoInformation.playing);
    }

    @Test
    public void returningToPreparedYandexAllowsPlayWithoutANewRequest() {
        yandex();
        init();
        load("a");
        ready(YANDEX);
        VideoInformation.setPlayerPlaying(false);
        init();
        load("a");
        assertEquals(1, VoiceOverTranslationPatch.starts);
        assertFalse(VideoInformation.playing);
        VideoInformation.setPlayerPlaying(true);
        assertTrue(VideoInformation.playing);
    }

    @Test
    public void returningToPreparedGoogleDoesNotRestartTranslation() {
        google();
        init();
        load("a");
        ready(GOOGLE);
        VideoInformation.setPlayerPlaying(false);
        init();
        load("a");
        assertEquals(1, GoogleVoiceOverTranslationPatch.starts);
        assertFalse(VideoInformation.playing);
        VideoInformation.setPlayerPlaying(true);
        assertTrue(VideoInformation.playing);
    }

    @Test
    public void completionDuringRecreationIsDeferred() {
        yandex();
        init();
        load("a");
        init();
        assertFalse(TranslationPlaybackController.ready(YANDEX, "a"));
        assertFalse(VideoInformation.playing);
        load("a");
        assertTrue(VideoInformation.playing);
    }

    @Test
    public void completionCannotResumeADifferentVideo() {
        yandex();
        init();
        load("a");
        init();
        TranslationPlaybackController.ready(YANDEX, "a");
        load("b");
        assertFalse(VideoInformation.playing);
        assertTrue(TranslationPlaybackController.isWaiting(YANDEX, "b"));
    }

    @Test
    public void newVideoHasItsOwnAutomaticRequest() {
        google();
        init();
        load("a");
        ready(GOOGLE);
        load("b");
        assertEquals(2, GoogleVoiceOverTranslationPatch.starts);
        assertTrue(TranslationPlaybackController.isWaiting(GOOGLE, "b"));
    }

    @Test
    public void cachedMetadataDoesNotClaimAnUnidentifiedPlayer() {
        yandex();
        init();
        load("a");
        ready(YANDEX);
        init();
        TranslationPlaybackController.metadataLoaded("a");
        Utils.drain();
        assertEquals(1, VoiceOverTranslationPatch.starts);
        assertFalse(state.matchesVideo("a"));
        assertFalse(
                TranslationPlaybackController.overridePlayWhenReady(VideoInformation.player, true));
    }

    @Test
    public void queuedMetadataCannotClaimAReplacementPlayer() {
        yandex();
        init();
        VideoInformation.id = VoiceOverTranslationPatch.pending = "a";
        TranslationPlaybackController.newVideoLoaded("a");
        init();
        Utils.drain();
        assertEquals(0, VoiceOverTranslationPatch.starts);
    }

    @Test
    public void delayedYandexMetadataRetriesAutomaticStart() {
        yandex();
        init();
        VideoInformation.id = "a";
        TranslationPlaybackController.newVideoLoaded("a");
        Utils.drain();
        assertEquals(0, VoiceOverTranslationPatch.starts);
        VoiceOverTranslationPatch.pending = "a";
        TranslationPlaybackController.metadataLoaded("a");
        Utils.drain();
        assertEquals(1, VoiceOverTranslationPatch.starts);
    }

    @Test
    public void metadataForANoncurrentVideoIsIgnored() {
        google();
        init();
        VideoInformation.id = "b";
        TranslationPlaybackController.newVideoLoaded("a");
        Utils.drain();
        assertEquals(0, GoogleVoiceOverTranslationPatch.starts);
    }

    @Test
    public void pauseOnlyModeWaitsForAManualTranslation() {
        yandex();
        Settings.VOT_AUTO_TRANSLATE.value = false;
        init();
        load("a");
        assertEquals(0, VoiceOverTranslationPatch.starts);
        assertFalse(
                TranslationPlaybackController.overridePlayWhenReady(VideoInformation.player, true));
    }

    @Test
    public void disabledPauseDoesNotHoldAutomaticTranslation() {
        yandex();
        Settings.VOT_PAUSE_VIDEO_WHILE_PREPARING_TRANSLATION.value = false;
        init();
        load("a");
        assertTrue(
                TranslationPlaybackController.overridePlayWhenReady(VideoInformation.player, true));
        assertFalse(TranslationPlaybackController.isWaiting(YANDEX, "a"));
    }

    @Test
    public void inactiveProviderDoesNotHold() {
        Settings.VOT_PAUSE_VIDEO_WHILE_PREPARING_TRANSLATION.value = true;
        init();
        assertTrue(
                TranslationPlaybackController.overridePlayWhenReady(VideoInformation.player, true));
        assertEquals(NONE, state.provider());
    }

    @Test
    public void automaticGoogleTakesPrecedenceOverManualYandex() {
        yandex();
        google();
        Settings.VOT_AUTO_TRANSLATE.value = false;
        init();
        load("a");
        assertEquals(GOOGLE, state.provider());
        assertEquals(1, GoogleVoiceOverTranslationPatch.starts);
        assertEquals(0, VoiceOverTranslationPatch.starts);
    }

    @Test
    public void automaticYandexHasDefaultPriorityWhenBothAreAutomatic() {
        yandex();
        google();
        init();
        load("a");
        assertEquals(YANDEX, state.provider());
        assertEquals(0, GoogleVoiceOverTranslationPatch.starts);
    }

    @Test
    public void manualGooglePauseHasPriorityOverEnabledYandex() {
        Settings.VOT_ENABLED.value = true;
        google();
        Settings.GOOGLE_VOT_AUTO_TRANSLATE.value = false;
        init();
        assertEquals(GOOGLE, state.provider());
    }

    @Test
    public void enabledProviderWithoutPauseIsStillSelected() {
        Settings.GOOGLE_VOT_ENABLED.value = true;
        init();
        assertTrue(TranslationPlaybackController.usesGoogle());
        assertFalse(state.isWaiting());
    }

    @Test
    public void enabledYandexWithoutPauseIsStillSelected() {
        Settings.VOT_ENABLED.value = true;
        init();
        assertEquals(YANDEX, state.provider());
        assertFalse(state.isWaiting());
    }

    @Test
    public void watchDetailsDoNotWaitForPausedPlayback() {
        yandex();
        assertEquals(0, TranslationPlaybackController.overrideWatchNextProcessingDelay(5000));
        Settings.VOT_ENABLED.value = false;
        assertEquals(5000, TranslationPlaybackController.overrideWatchNextProcessingDelay(5000));
        google();
        assertEquals(0, TranslationPlaybackController.overrideWatchNextProcessingDelay(5000));
    }

    @Test
    public void unrelatedPlayerIsNotIntercepted() {
        yandex();
        init();
        assertTrue(TranslationPlaybackController.overridePlayWhenReady(new Object(), true));
        assertTrue(Utils.queue.isEmpty());
    }

    @Test
    public void enforcedPausePreservesAutomaticResume() {
        yandex();
        init();
        load("a");
        TranslationPlaybackController.enforcePause();
        ready(YANDEX);
        assertTrue(VideoInformation.playing);
        assertTrue(VoiceOverTranslationPatch.paused > 0);
        assertTrue(GoogleVoiceOverTranslationPatch.paused > 0);
    }

    @Test
    public void manualPauseCancelsResume() {
        google();
        init();
        load("a");
        assertFalse(
                TranslationPlaybackController.overridePlayWhenReady(
                        VideoInformation.player, false));
        ready(GOOGLE);
        assertFalse(VideoInformation.playing);
    }

    @Test
    public void manualPlayWhileWaitingRequestsResume() {
        google();
        init();
        load("a");
        TranslationPlaybackController.overridePlayWhenReady(VideoInformation.player, false);
        assertFalse(
                TranslationPlaybackController.overridePlayWhenReady(VideoInformation.player, true));
        ready(GOOGLE);
        assertTrue(VideoInformation.playing);
    }

    @Test
    public void providerSwitchTransfersPauseOwnershipImmediately() {
        yandex();
        google();
        init();
        load("a");
        TranslationPlaybackController.select(GOOGLE, "a");
        assertTrue(VoiceOverTranslationPatch.suspended > 0);
        assertTrue(TranslationPlaybackController.usesGoogle());
        assertFalse(TranslationPlaybackController.ready(YANDEX, "a"));
        assertTrue(TranslationPlaybackController.isWaiting(GOOGLE, "a"));
        ready(GOOGLE);
        assertTrue(VideoInformation.playing);
    }

    @Test
    public void switchBackToYandexStopsGoogle() {
        yandex();
        google();
        init();
        load("a");
        TranslationPlaybackController.select(GOOGLE, "a");
        int old = GoogleVoiceOverTranslationPatch.suspended;
        TranslationPlaybackController.select(YANDEX, "a");
        assertEquals(old + 1, GoogleVoiceOverTranslationPatch.suspended);
        assertTrue(TranslationPlaybackController.isWaiting(YANDEX, "a"));
    }

    @Test
    public void staleReadyOrFailureDoesNotReleaseTheCurrentHold() {
        google();
        init();
        load("a");
        load("b");
        assertFalse(TranslationPlaybackController.ready(GOOGLE, "a"));
        TranslationPlaybackController.failed(GOOGLE, "a");
        assertTrue(TranslationPlaybackController.isWaiting(GOOGLE, "b"));
    }

    @Test
    public void failedRequestAllowsManualPlay() {
        yandex();
        init();
        load("a");
        TranslationPlaybackController.failed(YANDEX, "a");
        assertFalse(VideoInformation.playing);
        assertTrue(
                TranslationPlaybackController.overridePlayWhenReady(VideoInformation.player, true));
    }

    @Test
    public void unavailableBackendDoesNotKeepTheHold() {
        google();
        init();
        load("a");
        VideoInformation.available = false;
        assertFalse(TranslationPlaybackController.ready(GOOGLE, "a"));
        assertFalse(state.isWaiting());
    }

    @Test
    public void retiredPlayerCallbackCannotPauseSpeech() {
        google();
        init();
        load("a");
        int n = GoogleVoiceOverTranslationPatch.paused;
        TranslationPlaybackController.overridePlayWhenReady(VideoInformation.player, false);
        VideoInformation.player = new Object();
        Utils.drain();
        assertEquals(n, GoogleVoiceOverTranslationPatch.paused);
    }

    @Test
    public void pauseCallbackDoesNotStopAlreadyResumedSpeech() {
        google();
        init();
        load("a");
        int n = GoogleVoiceOverTranslationPatch.paused;
        TranslationPlaybackController.overridePlayWhenReady(VideoInformation.player, false);
        VideoInformation.playing = true;
        Utils.drain();
        assertEquals(n, GoogleVoiceOverTranslationPatch.paused);
    }

    @Test
    public void backendExceptionDoesNotLeaveInternalPauseFlagSet() {
        yandex();
        init();
        load("a");
        VideoInformation.fail = true;
        try {
            TranslationPlaybackController.enforcePause();
            fail();
        } catch (IllegalStateException expected) {
        }
        VideoInformation.fail = false;
        TranslationPlaybackController.overridePlayWhenReady(VideoInformation.player, false);
        ready(YANDEX);
        assertFalse(VideoInformation.playing);
    }

    @Test
    public void nativeMetadataFromCurrentDirectorStartsTranslation() {
        yandex();
        init();
        TranslationPlaybackController.nativeVideoLoaded(director, new VideoDetails("a"));
        Utils.drain();
        assertEquals("a", VideoInformation.id);
        assertEquals("a", GoogleVoiceOverTranslationPatch.id);
        assertEquals(1, VoiceOverTranslationPatch.starts);
    }

    @Test
    public void retiredDirectorMetadataIsIgnored() {
        yandex();
        init();
        TranslationPlaybackController.nativeVideoLoaded(new Object(), new VideoDetails("a"));
        Utils.drain();
        assertEquals("", VideoInformation.id);
    }

    @Test
    public void directorRetiredWhileCallbackQueuedIsIgnored() {
        yandex();
        init();
        TranslationPlaybackController.nativeVideoLoaded(director, new VideoDetails("a"));
        TranslationPlaybackController.initialize(new Object());
        Utils.drain();
        assertEquals("", VideoInformation.id);
    }

    @Test
    public void emptyOrInvalidNativeMetadataIsIgnored() {
        yandex();
        init();
        TranslationPlaybackController.nativeVideoLoaded(director, null);
        TranslationPlaybackController.nativeVideoLoaded(director, new Object());
        TranslationPlaybackController.nativeVideoLoaded(director, new VideoDetails(""));
        Utils.drain();
        assertEquals("", VideoInformation.id);
    }

    @Test
    public void voiceAndServiceChangesReachTheActiveGoogleEngine() {
        google();
        init();
        load("a");
        for (Settings.Flag f :
                new Settings.Flag[] {
                    Settings.GOOGLE_VOT_USE_NATIVE_TTS, Settings.GOOGLE_VOT_TTS_VOICE_TYPE
                }) TranslationPlaybackController.onSettingChanged(f.key);
        assertEquals(2, GoogleVoiceOverTranslationPatch.voiceChanges);
        for (Settings.Flag f :
                new Settings.Flag[] {
                    Settings.GOOGLE_VOT_TRANSLATION_SERVICE,
                    Settings.GOOGLE_VOT_CAPTION_LANGUAGE,
                    Settings.GOOGLE_VOT_OPENROUTER_MODEL
                }) TranslationPlaybackController.onSettingChanged(f.key);
        assertEquals(3, GoogleVoiceOverTranslationPatch.reloads);
    }

    @Test
    public void disablingYandexSelectsAutomaticGoogleOnCurrentVideo() {
        yandex();
        google();
        init();
        load("a");
        Settings.VOT_ENABLED.value = false;
        TranslationPlaybackController.onSettingChanged(Settings.VOT_ENABLED.key);
        Utils.drain();
        assertEquals(1, GoogleVoiceOverTranslationPatch.starts);
        assertTrue(TranslationPlaybackController.isWaiting(GOOGLE, "a"));
    }

    @Test
    public void disablingBothProvidersReleasesTheHold() {
        yandex();
        init();
        load("a");
        Settings.VOT_ENABLED.value = false;
        TranslationPlaybackController.onSettingChanged(Settings.VOT_ENABLED.key);
        Utils.drain();
        assertEquals(NONE, state.provider());
        assertTrue(
                TranslationPlaybackController.overridePlayWhenReady(VideoInformation.player, true));
        assertTrue(GoogleVoiceOverTranslationPatch.suspended > 0);
    }

    @Test
    public void disablingPauseReleasesHoldWithoutStartingPlayback() {
        google();
        init();
        load("a");
        Settings.GOOGLE_VOT_PAUSE_WHILE_PREPARING.value = false;
        TranslationPlaybackController.onSettingChanged(
                Settings.GOOGLE_VOT_PAUSE_WHILE_PREPARING.key);
        assertFalse(state.isWaiting());
        assertFalse(VideoInformation.playing);
    }

    @Test
    public void unrelatedSettingsDoNotRestartTranslation() {
        google();
        init();
        load("a");
        TranslationPlaybackController.onSettingChanged("irrelevant");
        Utils.drain();
        assertEquals(1, GoogleVoiceOverTranslationPatch.starts);
    }

    @Test
    public void manualYandexPauseWinsOverEnabledGoogle() {
        yandex();
        Settings.VOT_AUTO_TRANSLATE.value = false;
        Settings.GOOGLE_VOT_ENABLED.value = true;
        init();
        assertEquals(YANDEX, state.provider());
    }

    @Test
    public void disabledGoogleCannotClaimAutomaticPlayback() {
        yandex();
        Settings.VOT_AUTO_TRANSLATE.value = false;
        Settings.GOOGLE_VOT_AUTO_TRANSLATE.value = true;
        init();
        assertEquals(YANDEX, state.provider());
    }

    @Test
    public void manualGoogleDoesNotStartAnAutomaticRequest() {
        google();
        Settings.GOOGLE_VOT_AUTO_TRANSLATE.value = false;
        init();
        load("a");
        assertEquals(0, GoogleVoiceOverTranslationPatch.starts);
    }

    @Test
    public void changingPauseOffMakesWaitingQueryFalseBeforeStateCleanup() {
        google();
        init();
        load("a");
        Settings.GOOGLE_VOT_PAUSE_WHILE_PREPARING.value = false;
        assertFalse(TranslationPlaybackController.isWaiting(GOOGLE, "a"));
    }

    @Test
    public void enablingPauseDoesNotCancelTheCurrentHold() {
        yandex();
        init();
        load("a");
        TranslationPlaybackController.onSettingChanged(
                Settings.VOT_PAUSE_VIDEO_WHILE_PREPARING_TRANSLATION.key);
        assertTrue(state.isWaiting());
    }

    @Test
    public void disablingYandexPauseReleasesHold() {
        yandex();
        init();
        load("a");
        Settings.VOT_PAUSE_VIDEO_WHILE_PREPARING_TRANSLATION.value = false;
        TranslationPlaybackController.onSettingChanged(
                Settings.VOT_PAUSE_VIDEO_WHILE_PREPARING_TRANSLATION.key);
        assertFalse(state.isWaiting());
    }

    @Test
    public void settingsCannotClaimUnidentifiedPlayer() {
        yandex();
        init();
        load("a");
        init();
        Settings.VOT_ENABLED.value = false;
        TranslationPlaybackController.onSettingChanged(Settings.VOT_ENABLED.key);
        assertEquals(YANDEX, state.provider());
        assertFalse(state.matchesVideo("a"));
    }

    @Test
    public void enablingAutomaticYandexStartsOnCurrentVideo() {
        yandex();
        Settings.VOT_AUTO_TRANSLATE.value = false;
        init();
        load("a");
        Settings.VOT_AUTO_TRANSLATE.value = true;
        TranslationPlaybackController.onSettingChanged(Settings.VOT_AUTO_TRANSLATE.key);
        Utils.drain();
        assertEquals(1, VoiceOverTranslationPatch.starts);
    }

    @Test
    public void enablingAutomaticGoogleStartsOnCurrentVideo() {
        google();
        Settings.GOOGLE_VOT_AUTO_TRANSLATE.value = false;
        init();
        load("a");
        Settings.GOOGLE_VOT_AUTO_TRANSLATE.value = true;
        TranslationPlaybackController.onSettingChanged(Settings.GOOGLE_VOT_AUTO_TRANSLATE.key);
        Utils.drain();
        assertEquals(1, GoogleVoiceOverTranslationPatch.starts);
    }

    @Test
    public void enablingGoogleSelectsItImmediately() {
        init();
        load("a");
        google();
        TranslationPlaybackController.onSettingChanged(Settings.GOOGLE_VOT_ENABLED.key);
        Utils.drain();
        assertEquals(1, GoogleVoiceOverTranslationPatch.starts);
    }

    @Test
    public void disablingProvidersStopsEachSpeechBackend() {
        google();
        init();
        load("a");
        int n = GoogleVoiceOverTranslationPatch.suspended;
        Settings.GOOGLE_VOT_ENABLED.value = false;
        TranslationPlaybackController.onSettingChanged(Settings.GOOGLE_VOT_ENABLED.key);
        Utils.drain();
        assertTrue(GoogleVoiceOverTranslationPatch.suspended > n);
    }

    @Test
    public void readyReportsActualResume() {
        yandex();
        init();
        load("a");
        assertTrue(TranslationPlaybackController.ready(YANDEX, "a"));
        assertFalse(TranslationPlaybackController.ready(YANDEX, "a"));
    }

    @Test
    public void readyChecksCurrentBackendVideoEvenWhenOwnerMatches() {
        yandex();
        init();
        load("a");
        VideoInformation.id = "b";
        assertFalse(TranslationPlaybackController.ready(YANDEX, "a"));
        assertTrue(state.isWaiting(YANDEX, "a"));
    }

    @Test
    public void manualSelectionWhilePlayingPausesThenResumes() {
        yandex();
        init();
        load("a");
        ready(YANDEX);
        google();
        VideoInformation.playing = true;
        TranslationPlaybackController.select(GOOGLE, "a");
        assertFalse(VideoInformation.playing);
        assertTrue(TranslationPlaybackController.ready(GOOGLE, "a"));
    }

    @Test
    public void manualMetadataHoldPausesAnAlreadyPlayingBackend() {
        yandex();
        Settings.VOT_AUTO_TRANSLATE.value = false;
        init();
        VideoInformation.playing = true;
        load("a");
        assertFalse(VideoInformation.playing);
    }

    @Test
    public void unrelatedPlayerPauseIsPassedThroughWithoutSideEffects() {
        yandex();
        init();
        assertFalse(TranslationPlaybackController.overridePlayWhenReady(new Object(), false));
        assertTrue(Utils.queue.isEmpty());
    }

    @Test
    public void startingPlaybackDoesNotQueueSpeechPause() {
        yandex();
        init();
        load("a");
        ready(YANDEX);
        assertTrue(Utils.queue.isEmpty());
        assertTrue(
                TranslationPlaybackController.overridePlayWhenReady(VideoInformation.player, true));
        assertTrue(Utils.queue.isEmpty());
    }

    @Test
    public void automaticStartIsRetriedAfterAbandonedIntermediateVideo() {
        google();
        init();
        load("a");
        ready(GOOGLE);
        TranslationPlaybackController.newVideoLoaded("b");
        VideoInformation.id = "a";
        TranslationPlaybackController.newVideoLoaded("a");
        Utils.drain();
        assertEquals(2, GoogleVoiceOverTranslationPatch.starts);
    }

    @Test
    public void googleOwnershipIsFalseForYandex() {
        yandex();
        init();
        assertFalse(TranslationPlaybackController.usesGoogle());
    }

    @Test
    public void nativeMetadataIncludesEligibilityAndIdentityFields() {
        yandex();
        init();
        TranslationPlaybackController.nativeVideoLoaded(director, new VideoDetails("a"));
        Utils.drain();
        assertEquals("channel", VideoInformation.channel);
        assertEquals("author", VideoInformation.author);
        assertEquals("title", VideoInformation.title);
        assertEquals(100000L, VideoInformation.length);
        assertTrue(VideoInformation.live);
        assertEquals("channel", VoiceOverTranslationPatch.channel);
        assertEquals("author", VoiceOverTranslationPatch.author);
        assertEquals("title", VoiceOverTranslationPatch.title);
        assertEquals(100000L, VoiceOverTranslationPatch.length);
        assertTrue(VoiceOverTranslationPatch.live);
    }

    @Test
    public void backendControlsRequireMainThread() {
        Utils.mainThread = false;
        try {
            TranslationPlaybackController.select(GOOGLE, "a");
            fail();
        } catch (IllegalStateException expected) {
        }
        try {
            TranslationPlaybackController.ready(GOOGLE, "a");
            fail();
        } catch (IllegalStateException expected) {
        }
        try {
            TranslationPlaybackController.onSettingChanged("anything");
            fail();
        } catch (IllegalStateException expected) {
        } finally {
            Utils.mainThread = true;
        }
    }

    @Test
    public void bothManualProvidersPreferYandexPause() {
        yandex();
        google();
        Settings.VOT_AUTO_TRANSLATE.value = false;
        Settings.GOOGLE_VOT_AUTO_TRANSLATE.value = false;
        init();
        assertEquals(YANDEX, state.provider());
    }

    @Test
    public void enforcedPauseDoesNotRecordAnArtificialPlayRequest() {
        yandex();
        init();
        load("a");
        TranslationPlaybackController.overridePlayWhenReady(VideoInformation.player, false);
        TranslationPlaybackController.enforcePause();
        assertFalse(TranslationPlaybackController.ready(YANDEX, "a"));
    }

    @Test
    public void disabledPausePreventsEnforcementOfOldHold() {
        google();
        init();
        load("a");
        Settings.GOOGLE_VOT_PAUSE_WHILE_PREPARING.value = false;
        VideoInformation.playing = true;
        TranslationPlaybackController.enforcePause();
        assertTrue(VideoInformation.playing);
    }

    @Test
    public void waitingQueryRejectsTheWrongVideo() {
        google();
        init();
        load("a");
        assertFalse(TranslationPlaybackController.isWaiting(GOOGLE, "b"));
    }

    @Test
    public void emptyNativeMetadataDoesNotEraseTheCurrentVideo() {
        yandex();
        init();
        load("a");
        TranslationPlaybackController.nativeVideoLoaded(director, new VideoDetails(""));
        Utils.drain();
        assertEquals("a", VideoInformation.id);
    }

    @Test
    public void disabledGoogleCannotEnforceItsStoredPausePreference() {
        google();
        init();
        load("a");
        Settings.GOOGLE_VOT_ENABLED.value = false;
        assertTrue(
                TranslationPlaybackController.overridePlayWhenReady(VideoInformation.player, true));
        assertFalse(TranslationPlaybackController.isWaiting(GOOGLE, "a"));
    }

    @Test
    public void changingAutomaticSettingOnReadyVideoDoesNotRearmHold() {
        yandex();
        init();
        load("a");
        ready(YANDEX);
        Settings.VOT_AUTO_TRANSLATE.value = false;
        TranslationPlaybackController.onSettingChanged(Settings.VOT_AUTO_TRANSLATE.key);
        Utils.drain();
        assertTrue(VideoInformation.playing);
        assertFalse(state.isWaiting());
    }

    @Test
    public void changingCurrentProviderDoesNotSuspendNewlySelectedGoogle() {
        yandex();
        google();
        init();
        load("a");
        int n = GoogleVoiceOverTranslationPatch.suspended;
        Settings.VOT_ENABLED.value = false;
        TranslationPlaybackController.onSettingChanged(Settings.VOT_ENABLED.key);
        Utils.drain();
        assertEquals(n, GoogleVoiceOverTranslationPatch.suspended);
    }

    @Test
    public void staleSettingsWhileVideoMetadataDiffersCannotChangeOwner() {
        yandex();
        google();
        init();
        load("a");
        VideoInformation.id = "b";
        Settings.VOT_ENABLED.value = false;
        TranslationPlaybackController.onSettingChanged(Settings.VOT_ENABLED.key);
        assertTrue(state.isWaiting(YANDEX, "a"));
    }

    @Test
    public void completedAutomaticDispatchIsNotRepeated() {
        yandex();
        init();
        load("a");
        int n = VoiceOverTranslationPatch.attempts;
        TranslationPlaybackController.metadataLoaded("a");
        Utils.drain();
        assertEquals(n, VoiceOverTranslationPatch.attempts);
    }

    @Test
    public void manualProviderSelectionWinsOverPendingAutomaticGoogle() {
        yandex();
        google();
        Settings.VOT_AUTO_TRANSLATE.value = false;
        init();
        VideoInformation.id = VoiceOverTranslationPatch.pending = "a";
        TranslationPlaybackController.newVideoLoaded("a");
        TranslationPlaybackController.select(YANDEX, "a");
        Utils.drain();
        assertEquals(0, GoogleVoiceOverTranslationPatch.starts);
        assertTrue(state.isWaiting(YANDEX, "a"));
    }

    @Test
    public void cachedMetadataIsDiscardedBeforeQueueing() {
        yandex();
        init();
        TranslationPlaybackController.metadataLoaded("old");
        assertTrue(Utils.queue.isEmpty());
    }
}
