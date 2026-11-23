package app.morphe.extension.youtube.patches.voiceovertranslation;

import static app.morphe.extension.youtube.patches.voiceovertranslation.TranslationPlaybackState.*;

import static org.junit.Assert.*;

import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.shared.VideoInformation;

import org.junit.*;

public class YandexPauseMethodsTest {
    @Before
    public void before() throws Exception {
        new TranslationPlaybackControllerTest().before();
        Utils.background.clear();
        Settings.VOT_ENABLED.value = true;
        Settings.VOT_PAUSE_VIDEO_WHILE_PREPARING_TRANSLATION.value = true;
        VideoInformation.id = "a";
        TranslationPlaybackController.initialize(new Object());
        TranslationPlaybackController.newVideoLoaded("a");
        Utils.drain();
        VideoInformation.setPlayerPlaying(true);
        TranslationPlaybackController.select(YANDEX, "a");
        YandexPauseMethods.pendingVideoId = "a";
        YandexPauseMethods.translationRequestGeneration.set(1);
        YandexPauseMethods.isTranslating.set(false);
        YandexPauseMethods.shortsPlaybackPaused =
                YandexPauseMethods.active = YandexPauseMethods.RootView.shorts = false;
        YandexPauseMethods.clears =
                YandexPauseMethods.notified =
                        YandexPauseMethods.requests = YandexPauseMethods.toggles = 0;
        YandexPauseMethods.requestGeneration = 0;
        YandexPauseMethods.mediaPlayer.set(null);
        YandexPauseMethods.isPaused = false;
        YandexPauseMethods.mainHandler.removed = 0;
        VideoInformation.time = 0;
        VideoInformation.length = 0;
        YandexPauseMethods.pendingVideoLength = 100000;
        YandexPauseMethods.pendingIsLive = false;
        YandexPauseMethods.toast = null;
        Settings.VOT_SOURCE_LANGUAGE.value = "auto";
        Settings.VOT_TARGET_LANGUAGE.value = "ru";
    }

    private boolean held() {
        return TranslationPlaybackController.isWaiting(YANDEX, "a");
    }

    @Test public void completedVideoAndAudioStayStoppedUntilReplayOrBackwardSeek() {
        var audio = new YandexPauseMethods.MediaPlayer();
        YandexPauseMethods.mediaPlayer.set(audio);
        YandexPauseMethods.isPaused = true;
        VideoInformation.playing = true;
        VideoInformation.length = VideoInformation.time = 900000;
        YandexPauseMethods.resumeAudio(-1);
        assertFalse(audio.playing);
        VideoInformation.time = -1;
        YandexPauseMethods.resumeAudio(-1);
        assertFalse(audio.playing);
        VideoInformation.length = 2000000;
        VideoInformation.time = 1000000;
        YandexPauseMethods.resumeAudio(-1);
        YandexPauseMethods.resumeAudio(1000001);
        assertFalse(audio.playing);
        YandexPauseMethods.resumeAudio(999999);
        assertTrue(audio.playing);
        assertEquals(999999, audio.position);
        YandexPauseMethods.pauseAudio();
        YandexPauseMethods.resumeAudio(0);
        assertTrue(audio.playing);
        assertEquals(0, audio.position);
    }

    @Test
    public void restoredMiniplayerDoesNotPlaySpeechWhenPlayCommandSucceedsButVideoStaysIdle() throws Exception {
        for (boolean file : new boolean[] {false, true}) {
            before();
            VideoInformation.deferPlay = true;
            VideoInformation.time = 658000;
            var audio = new YandexPauseMethods.MediaPlayer();
            YandexPauseMethods.mediaPlayer.set(audio);
            if (file) YandexPauseMethods.filePrepared(audio, audio, 1, "a");
            else YandexPauseMethods.directPrepared(audio, audio, 1, "a");
            assertFalse(held());
            assertFalse(VideoInformation.playing);
            assertFalse("Speech started without video; file=" + file, audio.playing);
            assertEquals(0, audio.speedUpdates);
            assertEquals(0, audio.starts);
            VideoInformation.playing = true;
            YandexPauseMethods.resumeAudio(-1);
            assertTrue(audio.playing);
            assertEquals(658000, audio.position);
            assertEquals(1, audio.starts);
            YandexPauseMethods.pauseAudio();
            VideoInformation.playing = false;
            YandexPauseMethods.resumeAudio(659000);
            assertFalse(audio.playing);
            assertEquals(1, audio.starts);
            assertEquals(1, audio.speedUpdates);
            assertEquals(1.0f, audio.volume, 0);
            assertTrue(YandexPauseMethods.isPaused);
        }
    }

    private void prepare(boolean file, YandexPauseMethods.MediaPlayer audio) {
        if (file) YandexPauseMethods.filePrepared(audio, audio, 1, "a");
        else YandexPauseMethods.directPrepared(audio, audio, 1, "a");
    }

    @Test
    public void preparedSpeechPreservesManualPauseAndSeeksBeforeAnyPlayback() throws Exception {
        for (boolean file : new boolean[] {false, true}) {
            before();
            TranslationPlaybackController.overridePlayWhenReady(VideoInformation.player, false);
            var audio = new YandexPauseMethods.MediaPlayer();
            YandexPauseMethods.mediaPlayer.set(audio);
            VideoInformation.time = 1;
            prepare(file, audio);
            assertFalse(audio.playing);
            assertTrue(YandexPauseMethods.isPaused);
            assertEquals(1, audio.position);
            assertEquals(1, audio.seeks);
            assertEquals(1.0f, audio.volume, 0);
            assertEquals(file ? 0 : 1, YandexPauseMethods.mainHandler.removed);
            VideoInformation.playing = true;
            YandexPauseMethods.resumeAudio(0);
            assertEquals(0, audio.position);
            assertEquals(1, audio.speedUpdates);
            assertFalse(YandexPauseMethods.isPaused);
            YandexPauseMethods.resumeAudio(2000);
            assertEquals(1, audio.starts);
            assertEquals(0, audio.position);
            YandexPauseMethods.pauseAudio();
            assertTrue(YandexPauseMethods.isPaused);
            YandexPauseMethods.pauseAudio();
            assertEquals(1, audio.pauses);
            YandexPauseMethods.resumeAudio(4000);
            assertEquals(4000, audio.position);
            assertEquals(2, audio.starts);
        }
    }

    @Test
    public void obsoletePreparationCannotStartOrReplaceCurrentAudio() throws Exception {
        for (boolean file : new boolean[] {false, true}) {
            before();
            var old = new YandexPauseMethods.MediaPlayer();
            var replacement = new YandexPauseMethods.MediaPlayer();
            YandexPauseMethods.mediaPlayer.set(replacement);
            prepare(file, old);
            assertSame(replacement, YandexPauseMethods.mediaPlayer.get());
            assertEquals(0, old.starts);
            assertTrue(held());
            YandexPauseMethods.mediaPlayer.set(old);
            YandexPauseMethods.translationRequestGeneration.incrementAndGet();
            prepare(file, old);
            assertNull(YandexPauseMethods.mediaPlayer.get());
            assertEquals(0, old.starts);
            assertTrue(held());
        }
    }

    @Test
    public void uninitializedPositionDoesNotSeekAndAudioControlsTolerateMissingPlayer() throws Exception {
        for (boolean file : new boolean[] {false, true}) {
            before();
            TranslationPlaybackController.overridePlayWhenReady(VideoInformation.player, false);
            var audio = new YandexPauseMethods.MediaPlayer();
            YandexPauseMethods.mediaPlayer.set(audio);
            VideoInformation.time = -1;
            prepare(file, audio);
            assertEquals(0, audio.seeks);
            assertFalse(audio.playing);
            YandexPauseMethods.mediaPlayer.set(null);
            VideoInformation.playing = true;
            YandexPauseMethods.resumeAudio(0);
            YandexPauseMethods.pauseAudio();
            assertTrue(YandexPauseMethods.isPaused);
        }
    }

    @Test
    public void alreadyPlayingVideoStartsPreparedSpeech() throws Exception {
        for (boolean file : new boolean[] {false, true}) {
            before();
            var audio = new YandexPauseMethods.MediaPlayer();
            YandexPauseMethods.mediaPlayer.set(audio);
            if (file) YandexPauseMethods.filePrepared(audio, audio, 1, "a");
            else YandexPauseMethods.directPrepared(audio, audio, 1, "a");
            assertTrue(VideoInformation.playing);
            assertTrue(audio.playing);
            assertEquals(1, audio.starts);
            assertEquals(1, audio.speedUpdates);
            assertEquals(0, audio.seeks);
        }
    }

    @Test
    public void preparedAudioReleasesCurrentRequest() {
        assertTrue(YandexPauseMethods.resumeVideoAfterTranslationReady("a", 1));
        assertFalse(held());
        assertTrue(VideoInformation.playing);
    }

    @Test
    public void obsoleteRequestCannotResume() {
        YandexPauseMethods.translationRequestGeneration.set(2);
        assertFalse(YandexPauseMethods.resumeVideoAfterTranslationReady("a", 1));
        assertTrue(held());
    }

    @Test
    public void differentVideoCannotResume() {
        assertFalse(YandexPauseMethods.resumeVideoAfterTranslationReady("b", 1));
        assertTrue(held());
    }

    @Test
    public void retiredBackendVideoCannotResume() {
        VideoInformation.id = "b";
        assertFalse(YandexPauseMethods.isCurrentTranslationRequest(1, "a"));
        assertFalse(YandexPauseMethods.resumeVideoAfterTranslationReady("a", 1));
        assertTrue(held());
    }

    @Test
    public void manualPauseIsPreservedOnPreparation() {
        TranslationPlaybackController.overridePlayWhenReady(VideoInformation.player, false);
        assertFalse(YandexPauseMethods.resumeVideoAfterTranslationReady("a", 1));
        assertFalse(held());
        assertFalse(VideoInformation.playing);
    }

    @Test
    public void currentRequestPausesPlayingVideo() {
        TranslationPlaybackController.failed(YANDEX, "a");
        VideoInformation.playing = true;
        YandexPauseMethods.pauseVideoForTranslation("a", 1);
        assertTrue(held());
        assertFalse(VideoInformation.playing);
    }

    @Test
    public void obsoleteRequestCannotTakeThePause() {
        TranslationPlaybackController.failed(YANDEX, "a");
        VideoInformation.playing = true;
        YandexPauseMethods.pauseVideoForTranslation("a", 2);
        assertFalse(held());
        assertTrue(VideoInformation.playing);
    }

    @Test
    public void invalidRequestIdentitiesAreRejected() {
        assertFalse(YandexPauseMethods.isCurrentTranslationRequestGeneration(0, "a"));
        assertFalse(YandexPauseMethods.isCurrentTranslationRequestGeneration(1, null));
        assertFalse(YandexPauseMethods.isCurrentTranslationRequestGeneration(1, ""));
        assertFalse(YandexPauseMethods.isCurrentTranslationRequestGeneration(1, "b"));
        assertFalse(YandexPauseMethods.isCurrentTranslationRequestGeneration(2, "a"));
        assertTrue(YandexPauseMethods.isCurrentTranslationRequestGeneration(1, "a"));
    }

    @Test
    public void evenZeroCurrentGenerationIsNotARequest() {
        YandexPauseMethods.translationRequestGeneration.set(0);
        assertFalse(YandexPauseMethods.isCurrentTranslationRequestGeneration(0, "a"));
    }

    @Test
    public void emptyCurrentVideoIsNotARequest() {
        YandexPauseMethods.pendingVideoId = "";
        assertFalse(YandexPauseMethods.isCurrentTranslationRequestGeneration(1, ""));
    }

    @Test
    public void invalidationCancelsHoldAndRetiresRequest() {
        YandexPauseMethods.isTranslating.set(true);
        YandexPauseMethods.invalidateTranslationRequest(false);
        assertFalse(held());
        assertFalse(YandexPauseMethods.isTranslating.get());
        assertEquals(2, YandexPauseMethods.translationRequestGeneration.get());
        assertEquals(1, YandexPauseMethods.clears);
        assertFalse(YandexPauseMethods.resumeVideoAfterTranslationReady("a", 1));
        assertTrue(
                TranslationPlaybackController.overridePlayWhenReady(VideoInformation.player, true));
    }

    @Test
    public void cancellationCannotReleaseGoogleHold() {
        Settings.GOOGLE_VOT_ENABLED.value = true;
        Settings.GOOGLE_VOT_PAUSE_WHILE_PREPARING.value = true;
        TranslationPlaybackController.select(GOOGLE, "a");
        YandexPauseMethods.clearPausedVideoState();
        assertTrue(TranslationPlaybackController.isWaiting(GOOGLE, "a"));
    }

    @Test
    public void requestArmsPauseBeforeBackgroundWork() {
        TranslationPlaybackController.failed(YANDEX, "a");
        VideoInformation.playing = true;
        assertTrue(YandexPauseMethods.startTranslationRequest("a", "title", "en", "ru", 100, true));
        assertTrue(held());
        assertFalse(VideoInformation.playing);
        assertEquals(0, YandexPauseMethods.requests);
        assertTrue(YandexPauseMethods.isTranslating.get());
        assertEquals(2, YandexPauseMethods.translationRequestGeneration.get());
        assertEquals(1, YandexPauseMethods.notified);
        assertEquals(1, YandexPauseMethods.clears);
        Utils.background.remove().run();
        assertEquals(1, YandexPauseMethods.requests);
        assertEquals(2, YandexPauseMethods.requestGeneration);
    }

    @Test
    public void requestWithoutPauseLeavesPlayingVideoAlone() {
        TranslationPlaybackController.failed(YANDEX, "a");
        VideoInformation.playing = true;
        assertTrue(
                YandexPauseMethods.startTranslationRequest("a", "title", "en", "ru", 100, false));
        assertFalse(held());
        assertTrue(VideoInformation.playing);
    }

    @Test
    public void simultaneousRequestIsNotDuplicated() {
        YandexPauseMethods.isTranslating.set(true);
        assertFalse(
                YandexPauseMethods.startTranslationRequest("a", "title", "en", "ru", 100, true));
        assertTrue(Utils.background.isEmpty());
        assertEquals(1, YandexPauseMethods.translationRequestGeneration.get());
    }

    @Test
    public void originalPlayerMustBePlayingBeforeSpeechResumes() {
        VideoInformation.playing = false;
        assertFalse(YandexPauseMethods.shouldPlayTranslationAudio());
        VideoInformation.playing = true;
        assertTrue(YandexPauseMethods.shouldPlayTranslationAudio());
    }

    @Test
    public void shortsKeepTheirOwnPauseState() {
        YandexPauseMethods.RootView.shorts = true;
        VideoInformation.playing = false;
        assertTrue(YandexPauseMethods.shouldPlayTranslationAudio());
        YandexPauseMethods.shortsPlaybackPaused = true;
        VideoInformation.playing = true;
        assertFalse(YandexPauseMethods.shouldPlayTranslationAudio());
    }

    @Test
    public void automaticTranslationWaitsForMatchingMetadata() {
        assertFalse(YandexPauseMethods.startAutomaticTranslation("b"));
        assertEquals(1, YandexPauseMethods.translationRequestGeneration.get());
        assertTrue(YandexPauseMethods.startAutomaticTranslation("a"));
        assertEquals(2, YandexPauseMethods.translationRequestGeneration.get());
    }

    @Test
    public void automaticTranslationDoesNotToggleOffPreparedAudio() {
        YandexPauseMethods.active = true;
        assertTrue(YandexPauseMethods.startAutomaticTranslation("a"));
        assertEquals(1, YandexPauseMethods.translationRequestGeneration.get());
    }

    @Test
    public void automaticTranslationDoesNotDuplicatePendingRequest() {
        YandexPauseMethods.isTranslating.set(true);
        assertTrue(YandexPauseMethods.startAutomaticTranslation("a"));
        assertEquals(1, YandexPauseMethods.translationRequestGeneration.get());
    }

    @Test
    public void disabledYandexDoesNotRequestTranslation() {
        Settings.VOT_ENABLED.value = false;
        assertTrue(YandexPauseMethods.startAutomaticTranslation("a"));
        assertEquals(1, YandexPauseMethods.translationRequestGeneration.get());
    }

    @Test
    public void cancelWhileLoadingStopsRequestAndRestoresPlayback() {
        Settings.VOT_AUTO_TRANSLATE.value = true;
        YandexPauseMethods.isTranslating.set(true);
        assertTrue(YandexPauseMethods.isTranslationRequested());
        YandexPauseMethods.toggleTranslation();
        assertFalse(held());
        assertTrue(VideoInformation.playing);
        assertFalse(YandexPauseMethods.isTranslationRequested());
        assertFalse(YandexPauseMethods.isTranslating.get());
        assertEquals(2, YandexPauseMethods.translationRequestGeneration.get());
        assertEquals("localized:revanced_vot_stopped", YandexPauseMethods.toast);
        assertTrue(Settings.VOT_AUTO_TRANSLATE.get());
        assertTrue(Utils.background.isEmpty());
    }

    @Test
    public void cancelPreparedAudioStopsItBeforeVideoResumes() {
        var audio = new YandexPauseMethods.MediaPlayer();
        audio.playing = true;
        YandexPauseMethods.mediaPlayer.set(audio);
        YandexPauseMethods.active = true;
        YandexPauseMethods.toggleTranslation();
        assertFalse(audio.playing);
        assertNull(YandexPauseMethods.mediaPlayer.get());
        assertTrue(VideoInformation.playing);
        assertFalse(YandexPauseMethods.isTranslationRequested());
    }

    @Test
    public void strandedHoldCanBeCanceledEvenAfterWorkerFinished() {
        assertFalse(YandexPauseMethods.isTranslating.get());
        assertTrue(YandexPauseMethods.isTranslationRequested());
        YandexPauseMethods.toggleTranslation();
        assertFalse(held());
        assertTrue(VideoInformation.playing);
        assertTrue(TranslationPlaybackController.overridePlayWhenReady(VideoInformation.player, true));
    }

    @Test
    public void cancelPreservesManualPauseAndDoesNotRestartOnLateReady() {
        VideoInformation.setPlayerPlaying(false);
        YandexPauseMethods.toggleTranslation();
        assertFalse(held());
        assertFalse(VideoInformation.playing);
        assertFalse(YandexPauseMethods.resumeVideoAfterTranslationReady("a", 1));
        assertFalse(VideoInformation.playing);
    }

    @Test
    public void canceledRequestCanBeRetriedAndOldFailureCannotReleaseIt() {
        YandexPauseMethods.toggleTranslation();
        YandexPauseMethods.toggleTranslation();
        assertTrue(held());
        assertTrue(YandexPauseMethods.isTranslating.get());
        assertEquals(3, YandexPauseMethods.translationRequestGeneration.get());
        YandexPauseMethods.translationFailed(1, "a", "old-error");
        assertTrue(held());
        assertEquals("localized:revanced_vot_started", YandexPauseMethods.toast);
        assertEquals(3, YandexPauseMethods.translationRequestGeneration.get());
        YandexPauseMethods.translationFailed(3, "b", "wrong-video");
        assertTrue(held());
    }

    @Test
    public void currentFailureStopsAudioAndReleasesHoldOnlyOnce() {
        var audio = new YandexPauseMethods.MediaPlayer();
        audio.playing = true;
        YandexPauseMethods.mediaPlayer.set(audio);
        YandexPauseMethods.active = true;
        YandexPauseMethods.isTranslating.set(true);
        YandexPauseMethods.translationFailed(1, "a", "no-speech");
        assertFalse(held());
        assertFalse(audio.playing);
        assertTrue(VideoInformation.playing);
        assertFalse(YandexPauseMethods.isTranslationRequested());
        assertEquals("localized:no-speech", YandexPauseMethods.toast);
        int plays = VideoInformation.playCalls;
        YandexPauseMethods.translationFailed(1, "a", "duplicate");
        assertEquals(plays, VideoInformation.playCalls);
        assertEquals("localized:no-speech", YandexPauseMethods.toast);
    }

    @Test
    public void failureAfterManualPauseAllowsPlayWithoutForcingIt() {
        VideoInformation.setPlayerPlaying(false);
        YandexPauseMethods.translationFailed(1, "a", "error");
        assertFalse(VideoInformation.playing);
        assertFalse(held());
        VideoInformation.setPlayerPlaying(true);
        assertTrue(VideoInformation.playing);
    }

    @Test
    public void cancellationWhileRecreatingDefersResumeUntilSameVideoIsConfirmed() {
        TranslationPlaybackController.initialize(new Object());
        YandexPauseMethods.isTranslating.set(true);
        YandexPauseMethods.toggleTranslation();
        assertFalse(VideoInformation.playing);
        TranslationPlaybackController.newVideoLoaded("a");
        Utils.drain();
        assertTrue(VideoInformation.playing);
        assertFalse(held());
    }

    @Test
    public void activeToggleWorksWithoutHoldAndDisabledToggleDoesNothing() {
        TranslationPlaybackController.failed(YANDEX, "a");
        YandexPauseMethods.active = true;
        assertTrue(YandexPauseMethods.isTranslationRequested());
        Settings.VOT_ENABLED.value = false;
        YandexPauseMethods.toggleTranslation();
        assertTrue(YandexPauseMethods.active);
        assertNull(YandexPauseMethods.toast);
        Settings.VOT_ENABLED.value = true;
        YandexPauseMethods.toggleTranslation();
        assertFalse(YandexPauseMethods.active);
        assertFalse(VideoInformation.playing);
        assertFalse(YandexPauseMethods.isTranslationRequested());
    }

    @Test
    public void suspensionRetiresRequestWithoutResumingOtherProvider() {
        YandexPauseMethods.isTranslating.set(true);
        YandexPauseMethods.suspendTranslation();
        assertFalse(held());
        assertFalse(VideoInformation.playing);
        assertFalse(YandexPauseMethods.isTranslating.get());
    }

    @Test
    public void automaticTranslationStartsInsteadOfCancelingStartupHold() {
        assertTrue(held());
        YandexPauseMethods.startAutomaticTranslation("a");
        assertTrue(held());
        assertTrue(YandexPauseMethods.isTranslating.get());
        assertEquals("localized:revanced_vot_started", YandexPauseMethods.toast);
    }

    @Test
    public void unavailableVideoReleasesHoldWithoutStartingRequest() throws Exception {
        for (int reason = 0; reason < 3; reason++) {
            before();
            if (reason == 0) { YandexPauseMethods.pendingIsLive = true; YandexPauseMethods.pendingVideoLength = 0; }
            if (reason == 1) YandexPauseMethods.pendingVideoLength = 14400001;
            if (reason == 2) Settings.VOT_SOURCE_LANGUAGE.value = "ru";
            YandexPauseMethods.startAutomaticTranslation("a");
            assertFalse(held());
            assertTrue(VideoInformation.playing);
            assertTrue(Utils.background.isEmpty());
            assertEquals("localized:" + new String[] {"revanced_vot_unavailable_live", "revanced_vot_unavailable_too_long", "revanced_vot_unavailable_same_language"}[reason], YandexPauseMethods.toast);
        }
    }

    @Test
    public void finishedLivestreamAndFourHourBoundaryRemainTranslatable() throws Exception {
        for (long length : new long[] {1, 14400000}) {
            before();
            YandexPauseMethods.pendingIsLive = true;
            YandexPauseMethods.pendingVideoLength = length;
            YandexPauseMethods.startAutomaticTranslation("a");
            assertTrue(YandexPauseMethods.isTranslating.get());
            assertTrue(held());
        }
    }

    @Test
    public void emptyAndAutoSourceDoNotCountAsSameLanguage() throws Exception {
        for (String source : new String[] {"", "AUTO", "en"}) {
            before();
            Settings.VOT_SOURCE_LANGUAGE.value = source;
            if (!source.equals("en")) Settings.VOT_TARGET_LANGUAGE.value = source;
            YandexPauseMethods.startAutomaticTranslation("a");
            assertTrue(YandexPauseMethods.isTranslating.get());
        }
    }

    @Test
    public void manualStartRequiresMetadataAndDoesNotDuplicateRequest() throws Exception {
        for (String id : new String[] {null, ""}) {
            before();
            TranslationPlaybackController.failed(YANDEX, "a");
            YandexPauseMethods.pendingVideoId = id;
            YandexPauseMethods.startTranslation();
            assertTrue(Utils.background.isEmpty());
            assertNull(YandexPauseMethods.toast);
        }
        before();
        YandexPauseMethods.isTranslating.set(true);
        YandexPauseMethods.startTranslation();
        assertTrue(Utils.background.isEmpty());
        assertNull(YandexPauseMethods.toast);
    }

    @Test
    public void pendingRequestIsStillCancelableWithPauseOptionOff() {
        Settings.VOT_PAUSE_VIDEO_WHILE_PREPARING_TRANSLATION.value = false;
        YandexPauseMethods.isTranslating.set(true);
        assertTrue(YandexPauseMethods.isTranslationRequested());
        YandexPauseMethods.toggleTranslation();
        assertFalse(YandexPauseMethods.isTranslationRequested());
    }

    @Test
    public void zeroLengthNonLiveVideoIsNotAnOngoingLivestream() {
        YandexPauseMethods.pendingVideoLength = 0;
        YandexPauseMethods.startAutomaticTranslation("a");
        assertTrue(YandexPauseMethods.isTranslating.get());
        Utils.background.remove().run();
        assertEquals(0, YandexPauseMethods.requestedDuration, 0);
    }

    @Test
    public void startPassesVideoMetadataAndSecondsToRequest() {
        YandexPauseMethods.pendingVideoLength = 123456;
        YandexPauseMethods.pendingVideoTitle = "example";
        Settings.VOT_SOURCE_LANGUAGE.value = "en";
        Settings.VOT_TARGET_LANGUAGE.value = "ru";
        YandexPauseMethods.startAutomaticTranslation("a");
        Utils.background.remove().run();
        assertEquals(123.456, YandexPauseMethods.requestedDuration, 0.000001);
        assertEquals("a", YandexPauseMethods.requestedId);
        assertEquals("example", YandexPauseMethods.requestedTitle);
        assertEquals("en", YandexPauseMethods.requestedSource);
        assertEquals("ru", YandexPauseMethods.requestedTarget);
    }
}
