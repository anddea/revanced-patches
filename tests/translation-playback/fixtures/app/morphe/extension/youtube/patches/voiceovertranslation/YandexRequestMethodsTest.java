package app.morphe.extension.youtube.patches.voiceovertranslation;

import static org.junit.Assert.*;
import static app.morphe.extension.youtube.patches.voiceovertranslation.TranslationPlaybackState.*;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.shared.VideoInformation;
import org.junit.*;

public class YandexRequestMethodsTest {
    @Before public void before() throws Exception {
        new YandexPauseMethodsTest().before();
        YandexRequestMethods.audioStarts = YandexRequestMethods.uploads = YandexRequestMethods.waits = 0;
        YandexRequestMethods.uploadError = false;
        YandexRequestMethods.duringUpload = () -> {};
        YandexRequestMethods.VotApiClient.cancelled = YandexRequestMethods.VotApiClient.retry = false;
        YandexRequestMethods.VotApiClient.error = YandexRequestMethods.VotApiClient.livelyUnavailable = false;
        YandexRequestMethods.VotApiClient.requests = YandexRequestMethods.VotApiClient.polls = YandexRequestMethods.VotApiClient.pollAction = 0;
        YandexRequestMethods.VotApiClient.beforeReturn = () -> {};
        YandexRequestMethods.VotApiClient.response = null;
        YandexRequestMethods.isTranslating.set(true);
        Utils.drain();
    }
    private void response(int status, String url) {
        YandexRequestMethods.VotApiClient.response = new YandexRequestMethods.VotApiClient.TranslationResult(status, url, 10, "no speech", "translation");
    }
    private void request() {
        YandexRequestMethods.requestTranslation("a", "title", "en", "ru", 123, 1, new YandexRequestMethods.VotAudioUploadState());
    }
    private boolean held() { return TranslationPlaybackController.isWaiting(YANDEX, "a"); }

    @Test public void everyTerminalResponseReleasesHoldAndShowsItsReason() throws Exception {
        for (int status : new int[] {-1, 0, 1, 4, 6, 99, 101, 104}) {
            before();
            if (status != -1) response(status >= 100 ? status - 100 : status, status >= 100 ? "" : null);
            request(); Utils.drain();
            assertFalse(held()); assertTrue(VideoInformation.playing);
            assertFalse(YandexRequestMethods.isTranslating.get());
            assertEquals("localized:" + (status == 6 ? "revanced_vot_auth_required" : "revanced_vot_playback_error"), YandexRequestMethods.toast);
        }
    }
    @Test public void queuedFailureCannotCancelRetryOrShowObsoleteToast() throws Exception {
        for (int status : new int[] {-1, 0, 1, 4, 6}) {
            before(); if (status != -1) response(status, ""); request();
            YandexRequestMethods.toggleTranslation();
            YandexRequestMethods.toggleTranslation();
            Utils.drain();
            assertTrue(held()); assertFalse(VideoInformation.playing);
            assertEquals("localized:revanced_vot_started", YandexRequestMethods.toast);
        }
    }
    @Test public void exceptionsReleaseOnlyCurrentRequest() throws Exception {
        for (boolean stale : new boolean[] {false, true}) {
            before(); YandexRequestMethods.VotApiClient.error = true; request();
            if (stale) YandexRequestMethods.translationRequestGeneration.incrementAndGet();
            Utils.drain(); assertEquals(stale, held());
        }
    }
    @Test public void canceledNetworkResponseCannotStartAudio() {
        response(1, "audio");
        YandexRequestMethods.VotApiClient.beforeReturn = () -> YandexRequestMethods.toggleTranslation();
        request(); Utils.drain();
        assertEquals(0, YandexRequestMethods.audioStarts);
        assertFalse(held()); assertTrue(VideoInformation.playing);
    }
    @Test public void alreadyRetiredRequestNeverContactsService() {
        YandexRequestMethods.translationRequestGeneration.incrementAndGet();
        request(); Utils.drain();
        assertEquals(0, YandexRequestMethods.VotApiClient.requests);
    }
    @Test public void finishedAudioUsesSelectedRouteAndRetainsHoldUntilPrepared() throws Exception {
        for (int status : new int[] {1, 4}) for (boolean proxy : new boolean[] {false, true}) {
            before(); response(status, "audio"); Settings.VOT_AUDIO_PROXY_ENABLED.value = proxy;
            request(); Utils.drain();
            assertEquals(1, YandexRequestMethods.audioStarts);
            assertEquals(proxy ? "proxy:audio" : "audio", YandexRequestMethods.startedUrl);
            assertEquals(proxy ? "audio" : null, YandexRequestMethods.fallbackUrl);
            assertTrue(held());
        }
    }
    @Test public void pollFailureAndAuthorizationAndExhaustionReleaseHold() throws Exception {
        for (int status : new int[] {2, 3, 5}) for (int action : new int[] {0, 1, 2}) {
            before(); response(status, null); YandexRequestMethods.VotApiClient.pollAction = action;
            request(); Utils.drain();
            assertFalse(held()); assertTrue(VideoInformation.playing);
            assertEquals(1, YandexRequestMethods.VotApiClient.polls);
            assertEquals(status == 5 ? 1 : 0, YandexRequestMethods.uploads);
            assertEquals("localized:" + new String[] {"revanced_vot_stream_not_ready", "revanced_vot_playback_error", "revanced_vot_auth_required"}[action], YandexRequestMethods.toast);
        }
    }
    @Test public void queuedPollFailuresCannotReleaseNewerRequest() throws Exception {
        for (int action : new int[] {0, 1, 2}) {
            before(); response(2, null); YandexRequestMethods.VotApiClient.pollAction = action;
            request(); YandexRequestMethods.translationRequestGeneration.incrementAndGet();
            Utils.drain(); assertTrue(held()); assertFalse(VideoInformation.playing);
        }
    }
    @Test public void uploadExceptionReleasesCurrentHold() {
        response(5, null); YandexRequestMethods.uploadError = true;
        request(); Utils.drain();
        assertFalse(held()); assertTrue(VideoInformation.playing);
        assertEquals("localized:revanced_vot_playback_error", YandexRequestMethods.toast);
    }
    @Test public void pollAudioCompletionUsesProxyAndCanBeCanceledBeforeDispatch() throws Exception {
        for (boolean proxy : new boolean[] {false, true}) for (boolean canceled : new boolean[] {false, true}) {
            before(); response(2, "audio"); YandexRequestMethods.VotApiClient.pollAction = 3;
            Settings.VOT_AUDIO_PROXY_ENABLED.value = proxy;
            request(); if (canceled) YandexRequestMethods.toggleTranslation(); Utils.drain();
            assertEquals(canceled ? 0 : 1, YandexRequestMethods.audioStarts);
            if (!canceled) {
                assertEquals(proxy ? "proxy:audio" : "audio", YandexRequestMethods.startedUrl);
                assertTrue(held());
            }
        }
    }

    @Test public void waitingAnnouncementsAndPollingIntervalsUseTheServiceEstimate() throws Exception {
        for (int status : new int[] {2, 3, 5}) for (int seconds : new int[] {-1, 0, 1, 12}) {
            before();
            YandexRequestMethods.VotApiClient.response = new YandexRequestMethods.VotApiClient.TranslationResult(status, null, seconds, "no speech", "translation");
            request();
            assertEquals(seconds, YandexRequestMethods.waits);
            assertEquals(seconds > 0 ? seconds : status == 5 ? 10 : 5, YandexRequestMethods.VotApiClient.waitTime);
            Utils.queue.remove().run();
            assertEquals(seconds > 0 ? "localized:revanced_vot_stream_waiting[" + seconds + " sec]" : "localized:revanced_vot_stream_waiting_status", YandexRequestMethods.toast);
        }
    }
    @Test public void livelyVoiceFallbackRetriesOnceAndReportsAvailability() throws Exception {
        for (boolean knownUnavailable : new boolean[] {false, true}) {
            before(); response(0, null);
            Settings.VOT_USE_LIVE_VOICES.value = true;
            YandexRequestMethods.VotApiClient.livelyUnavailable = knownUnavailable;
            request();
            assertEquals(knownUnavailable ? 2 : 1, YandexRequestMethods.VotApiClient.requests);
            assertEquals(!knownUnavailable, Settings.VOT_USE_LIVE_VOICES.get());
            Utils.queue.remove().run();
            assertEquals("localized:" + (knownUnavailable ? "revanced_vot_live_voices_unavailable" : "revanced_vot_playback_error"), YandexRequestMethods.toast);
        }
    }
    @Test public void pollHandlerChecksOwnershipForEveryFailureAndUpload() throws Exception {
        for (int action : new int[] {1, 2, 4, 5}) {
            before(); response(2, null); YandexRequestMethods.VotApiClient.pollAction = action;
            request();
            var handler = YandexRequestMethods.VotApiClient.handler;
            assertFalse(handler.isCancelled());
            if (action == 1) assertFalse(YandexRequestMethods.VotApiClient.retry);
            if (action == 4) assertEquals(1, YandexRequestMethods.uploads);
            if (action == 5) assertEquals(12, YandexRequestMethods.waits);
            Utils.drain();
            YandexRequestMethods.translationRequestGeneration.incrementAndGet();
            assertTrue(handler.isCancelled());
            int queued = Utils.queue.size(), uploads = YandexRequestMethods.uploads;
            assertFalse(handler.onFailed(YandexRequestMethods.VotApiClient.response));
            handler.onSessionRequired(); handler.onAudioRequested("https://youtu.be/a", "translation");
            assertEquals(queued, Utils.queue.size()); assertEquals(uploads, YandexRequestMethods.uploads);
        }
    }
    @Test public void livelyVoicePollingRequestsFallbackOnlyForItsSpecificError() throws Exception {
        for (boolean knownUnavailable : new boolean[] {false, true}) {
            before(); response(2, null);
            Settings.VOT_USE_LIVE_VOICES.value = true;
            YandexRequestMethods.VotApiClient.livelyUnavailable = knownUnavailable;
            YandexRequestMethods.VotApiClient.pollAction = 1;
            request();
            assertEquals(knownUnavailable, YandexRequestMethods.VotApiClient.retry);
            assertEquals(!knownUnavailable, Settings.VOT_USE_LIVE_VOICES.get());
            Utils.queue.remove().run(); Utils.queue.remove().run();
            assertEquals("localized:" + (knownUnavailable ? "revanced_vot_live_voices_unavailable" : "revanced_vot_playback_error"), YandexRequestMethods.toast);
        }
    }
    @Test public void cancellationDuringUploadPreventsPollingAndObsoleteFailure() throws Exception {
        for (boolean error : new boolean[] {false, true}) {
            before(); response(5, null); YandexRequestMethods.uploadError = error;
            YandexRequestMethods.duringUpload = () -> YandexRequestMethods.translationRequestGeneration.incrementAndGet();
            request(); assertEquals(0, YandexRequestMethods.VotApiClient.polls);
            assertEquals(1, Utils.queue.size());
        }
    }
    @Test public void directStaleUploadDoesNoWorkAndStalePollSeesCancellation() {
        response(2, null); YandexRequestMethods.translationRequestGeneration.incrementAndGet();
        YandexRequestMethods.handleAudioRequested(1, "a", "https://youtu.be/a", "translation", 123, "en", "ru", "title", 10, new YandexRequestMethods.VotAudioUploadState());
        assertEquals(0, YandexRequestMethods.uploads);
        YandexRequestMethods.pollTranslation(1, "a", "title", "https://youtu.be/a", 123, "en", "ru", 10, new YandexRequestMethods.VotAudioUploadState());
        assertTrue(YandexRequestMethods.VotApiClient.cancelled); assertTrue(Utils.queue.isEmpty());
    }
    @Test public void finishedWorkerClearsOnlyItsOwnProgressBeforeAudioPreparation() throws Exception {
        for (boolean replacement : new boolean[] {false, true}) {
            before(); response(1, "audio");
            YandexRequestMethods.VotApiClient.beforeReturn = () -> { if (replacement) YandexRequestMethods.translationRequestGeneration.incrementAndGet(); };
            request();
            assertEquals(replacement, YandexRequestMethods.isTranslating.get());
            assertEquals(replacement ? 0 : 1, YandexRequestMethods.clears);
        }
    }

    @Test public void disabledLiveVoicesDoNotRetryLivelyServiceErrors() {
        response(2, null); YandexRequestMethods.VotApiClient.pollAction = 1;
        YandexRequestMethods.VotApiClient.livelyUnavailable = true;
        request(); Utils.drain();
        assertFalse(YandexRequestMethods.VotApiClient.retry);
        assertFalse(held());
    }
    @Test public void obsoleteWaitingResponseCannotShowAnAnnouncementOrUpload() throws Exception {
        for (int status : new int[] {2, 5}) {
            before(); response(status, null);
            YandexRequestMethods.VotApiClient.beforeReturn = () -> YandexRequestMethods.toggleTranslation();
            request(); Utils.drain();
            assertEquals("localized:revanced_vot_stopped", YandexRequestMethods.toast);
            assertEquals(0, YandexRequestMethods.uploads);
            assertEquals(0, YandexRequestMethods.VotApiClient.polls);
        }
    }
}
