package app.morphe.extension.youtube.patches.voiceovertranslation;

import static app.morphe.extension.youtube.patches.voiceovertranslation.TranslationPlaybackState.*;

import static org.junit.Assert.*;

import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.shared.VideoInformation;

import org.junit.*;

import java.io.File;
import java.util.*;

public class GooglePauseMethodsTest {
    @Before
    public void before() throws Exception {
        new TranslationPlaybackControllerTest().before();
        Settings.GOOGLE_VOT_ENABLED.value = true;
        Settings.GOOGLE_VOT_PAUSE_WHILE_PREPARING.value = true;
        VideoInformation.id = "a";
        VideoInformation.time = 0;
        TranslationPlaybackController.initialize(new Object());
        TranslationPlaybackController.newVideoLoaded("a");
        Utils.drain();
        GooglePauseMethods.currentVideoId = "a";
        GooglePauseMethods.lang = "ru";
        GooglePauseMethods.voice = "edge";
        GooglePauseMethods.sessionEnabled = true;
        GooglePauseMethods.ttsReady = false;
        GooglePauseMethods.isLoading = true;
        GooglePauseMethods.transcriptGeneration = 1;
        GooglePauseMethods.lastSpokenIndex = 0;
        GooglePauseMethods.languages =
                GooglePauseMethods.cacheUpdates =
                        GooglePauseMethods.stopped =
                                GooglePauseMethods.loads =
                                        GooglePauseMethods.resets =
                                                GooglePauseMethods.notifications = 0;
        GooglePauseMethods.segments = new ArrayList<>(List.of(new TranscriptSegment()));
        GooglePauseMethods.nativeStartupAudio = null;
        GooglePauseMethods.tts = new GooglePauseMethods.FakeTts();
        GooglePauseMethods.ttsEngine = new GooglePauseMethods.FakeEngine();
        TtsPrefetcher.time = -1;
        TtsPrefetcher.updates = TtsPrefetcher.clears = 0;
        TtsPrefetcher.video = "";
        TranscriptTranslator.awaiting = false;
        TranscriptTranslator.aborted = 0;
        TtsCache.audio = null;
        TtsCache.id = "a";
        TtsCache.index = 0;
        TtsCache.voice = "edge";
        TtsCache.lang = "ru";
        TtsCache.text = "hello";
        Utils.background.clear();
        GooglePauseMethods.PlayerType.current = GooglePauseMethods.PlayerType.MAXIMIZED;
        GooglePauseMethods.MediaMetadataRetriever.duration = "1200";
    }

    private GooglePauseMethods.NativeStartupAudio nativeAudio(byte[] data) throws Exception {
        GooglePauseMethods.voice = "system";
        GooglePauseMethods.ttsReady = true;
        var audio = prepared();
        java.nio.file.Files.write(audio.file.toPath(), data);
        GooglePauseMethods.nativeStartupAudio = audio;
        return audio;
    }

    private void finishNative() {
        GooglePauseMethods.nativeStartupFinished("prepare", true);
        Utils.drain();
        assertFalse(Utils.background.isEmpty());
        Utils.background.remove().run();
        Utils.drain();
    }

    @Test
    public void nativeCompletionReadsBytesBeforeRelease() throws Exception {
        var audio = nativeAudio(new byte[] {1, 2, 3});
        finishNative();
        assertFalse(held());
        assertTrue(VideoInformation.playing);
        assertArrayEquals(new byte[] {1, 2, 3}, audio.bytes);
        assertEquals(1200, GooglePauseMethods.segments.get(0).durationMs);
        assertFalse(audio.file.exists());
    }

    @Test
    public void nativeCompletionKeepsManualPause() throws Exception {
        nativeAudio(new byte[] {1});
        VideoInformation.setPlayerPlaying(false);
        finishNative();
        assertFalse(held());
        assertFalse(VideoInformation.playing);
    }

    @Test
    public void missingNativeCompletionIsIgnored() {
        GooglePauseMethods.nativeStartupFinished("prepare", true);
        Utils.drain();
        assertTrue(held());
        assertTrue(Utils.background.isEmpty());
    }

    @Test
    public void unrelatedNativeCompletionIsIgnored() throws Exception {
        var audio = nativeAudio(new byte[] {1});
        GooglePauseMethods.nativeStartupFinished("other", true);
        Utils.drain();
        assertSame(audio, GooglePauseMethods.nativeStartupAudio);
        assertTrue(held());
        assertTrue(Utils.background.isEmpty());
    }

    @Test
    public void failedNativeCompletionReleasesHold() throws Exception {
        var audio = nativeAudio(new byte[] {1});
        GooglePauseMethods.nativeStartupFinished("prepare", false);
        Utils.drain();
        assertFalse(held());
        assertFalse(VideoInformation.playing);
        assertNull(GooglePauseMethods.nativeStartupAudio);
        assertFalse(audio.file.exists());
    }

    @Test
    public void staleNativeGenerationIsRejected() throws Exception {
        nativeAudio(new byte[] {1});
        GooglePauseMethods.transcriptGeneration++;
        GooglePauseMethods.nativeStartupFinished("prepare", true);
        Utils.drain();
        assertFalse(held());
        assertTrue(Utils.background.isEmpty());
    }

    @Test
    public void staleNativeVideoIsRejected() throws Exception {
        nativeAudio(new byte[] {1});
        GooglePauseMethods.currentVideoId = "b";
        GooglePauseMethods.nativeStartupFinished("prepare", true);
        Utils.drain();
        assertFalse(held());
        assertTrue(Utils.background.isEmpty());
    }

    @Test
    public void emptyNativeAudioReleasesHold() throws Exception {
        nativeAudio(new byte[0]);
        finishNative();
        assertFalse(held());
        assertFalse(VideoInformation.playing);
    }

    @Test
    public void missingNativeFileReleasesHold() throws Exception {
        var audio = nativeAudio(new byte[] {1});
        audio.file.delete();
        finishNative();
        assertFalse(held());
        assertNull(GooglePauseMethods.nativeStartupAudio);
    }

    @Test
    public void replacedNativeResultCannotReleaseNewHold() throws Exception {
        nativeAudio(new byte[] {1});
        GooglePauseMethods.nativeStartupFinished("prepare", true);
        Utils.drain();
        Utils.background.remove().run();
        var replacement = nativeAudio(new byte[] {2});
        Utils.drain();
        assertTrue(held());
        assertSame(replacement, GooglePauseMethods.nativeStartupAudio);
        assertNull(replacement.bytes);
    }

    @Test
    public void generationChangesDuringNativeReadAreRejected() throws Exception {
        var audio = nativeAudio(new byte[] {1});
        GooglePauseMethods.nativeStartupFinished("prepare", true);
        Utils.drain();
        Utils.background.remove().run();
        GooglePauseMethods.transcriptGeneration++;
        Utils.drain();
        assertTrue(held());
        assertSame(audio, GooglePauseMethods.nativeStartupAudio);
        assertNull(audio.bytes);
    }

    @Test
    public void removedSegmentDoesNotCrashNativeCompletion() throws Exception {
        var audio = nativeAudio(new byte[] {1});
        GooglePauseMethods.segments.clear();
        finishNative();
        assertArrayEquals(new byte[] {1}, audio.bytes);
    }

    @Test
    public void missingDurationDoesNotDiscardUsableAudio() throws Exception {
        nativeAudio(new byte[] {1});
        GooglePauseMethods.MediaMetadataRetriever.duration = null;
        finishNative();
        assertFalse(held());
        assertEquals(-1, GooglePauseMethods.segments.get(0).durationMs);
    }

    @Test
    public void googleToggleOffCancelsPreparation() throws Exception {
        var audio = nativeAudio(new byte[] {1});
        GooglePauseMethods.toggleTranslation();
        assertFalse(held());
        assertFalse(GooglePauseMethods.sessionEnabled);
        assertFalse(Settings.GOOGLE_VOT_SESSION_ENABLED.get());
        assertFalse(audio.file.exists());
        assertEquals(-1, GooglePauseMethods.lastSpokenIndex);
    }

    @Test
    public void googleToggleOnLoadsOnlyWhenNeeded() {
        GooglePauseMethods.sessionEnabled = false;
        GooglePauseMethods.isLoading = false;
        GooglePauseMethods.segments.clear();
        GooglePauseMethods.toggleTranslation();
        assertTrue(held());
        assertEquals(1, GooglePauseMethods.loads);
        assertTrue(Settings.GOOGLE_VOT_SESSION_ENABLED.get());
    }

    @Test
    public void googleDisabledToggleIsIgnored() {
        Settings.GOOGLE_VOT_ENABLED.value = false;
        GooglePauseMethods.toggleTranslation();
        assertTrue(GooglePauseMethods.sessionEnabled);
        assertEquals(0, GooglePauseMethods.notifications);
    }

    @Test
    public void sameGoogleVideoRetainsPreparedAudio() throws Exception {
        var audio = nativeAudio(new byte[] {1});
        GooglePauseMethods.lastVideoTimeMs = 100;
        GooglePauseMethods.wasExplicitSeek = true;
        GooglePauseMethods.newVideoLoaded("a");
        assertSame(audio, GooglePauseMethods.nativeStartupAudio);
        assertEquals(1, GooglePauseMethods.transcriptGeneration);
        assertEquals(0, GooglePauseMethods.loads);
        assertEquals(0, GooglePauseMethods.lastVideoTimeMs);
        assertFalse(GooglePauseMethods.wasExplicitSeek);
        assertEquals(-1, GooglePauseMethods.lastSpokenIndex);
    }

    @Test
    public void newGoogleVideoRetiresPreparationAndRearmsAutomaticHold() throws Exception {
        var audio = nativeAudio(new byte[] {1});
        Settings.GOOGLE_VOT_AUTO_TRANSLATE.value = true;
        GooglePauseMethods.httpErrorDialogShownThisVideo = true;
        GooglePauseMethods.newVideoLoaded("b");
        assertEquals("b", GooglePauseMethods.currentVideoId);
        assertFalse(audio.file.exists());
        assertEquals(2, GooglePauseMethods.transcriptGeneration);
        assertTrue(GooglePauseMethods.sessionEnabled);
        assertTrue(GooglePauseMethods.segments.isEmpty());
        assertEquals(1, GooglePauseMethods.loads);
        assertEquals(1, TranscriptTranslator.aborted);
        assertFalse(GooglePauseMethods.httpErrorDialogShownThisVideo);
        assertTrue(Settings.GOOGLE_VOT_SESSION_ENABLED.get());
        assertEquals(1, GooglePauseMethods.stopped);
        assertEquals(1, GooglePauseMethods.cacheUpdates);
        assertEquals("b", TtsPrefetcher.video);
        assertTrue(TranslationPlaybackController.isWaiting(GOOGLE, "b"));
        assertEquals(1, Utils.background.size());
        Utils.background.remove().run();
        assertEquals(1, GooglePauseMethods.ttsEngine.warmups);
    }

    @Test
    public void nativeNewVideoSkipsEdgeWarmup() {
        Settings.GOOGLE_VOT_AUTO_TRANSLATE.value = true;
        Settings.GOOGLE_VOT_USE_NATIVE_TTS.value = true;
        GooglePauseMethods.newVideoLoaded("b");
        assertEquals(1, GooglePauseMethods.loads);
        assertTrue(Utils.background.isEmpty());
    }

    @Test
    public void inlineNewVideoDoesNotStartTranslation() {
        Settings.GOOGLE_VOT_AUTO_TRANSLATE.value = true;
        GooglePauseMethods.PlayerType.current = GooglePauseMethods.PlayerType.INLINE_MINIMAL;
        GooglePauseMethods.newVideoLoaded("b");
        assertEquals(0, GooglePauseMethods.loads);
    }

    @Test
    public void manualNewVideoDoesNotStartTranslation() {
        GooglePauseMethods.newVideoLoaded("b");
        assertFalse(GooglePauseMethods.sessionEnabled);
        assertEquals(0, GooglePauseMethods.loads);
    }

    @Test
    public void disabledGoogleNewVideoDoesNotLoad() {
        Settings.GOOGLE_VOT_ENABLED.value = false;
        Settings.GOOGLE_VOT_AUTO_TRANSLATE.value = true;
        GooglePauseMethods.newVideoLoaded("b");
        assertEquals(0, GooglePauseMethods.loads);
    }

    @Test
    public void yandexOwnershipDoesNotAutostartGoogle() {
        TranslationPlaybackController.select(YANDEX, "a");
        Settings.GOOGLE_VOT_AUTO_TRANSLATE.value = true;
        GooglePauseMethods.newVideoLoaded("b");
        assertFalse(GooglePauseMethods.sessionEnabled);
        assertEquals(0, GooglePauseMethods.loads);
    }

    @Test
    public void googleToggleOnUsesExistingReadyAudioAtCurrentPosition() {
        TranslationPlaybackController.failed(GOOGLE, "a");
        GooglePauseMethods.sessionEnabled = false;
        GooglePauseMethods.isLoading = false;
        TtsCache.audio = new byte[] {1};
        GooglePauseMethods.toggleTranslation();
        assertEquals(0, GooglePauseMethods.loads);
        assertFalse(held());
        assertEquals("a", TtsPrefetcher.video);
        assertEquals(1, GooglePauseMethods.notifications);
    }

    @Test
    public void googleToggleOnArmsACompletedHoldAgain() {
        TranslationPlaybackController.failed(GOOGLE, "a");
        GooglePauseMethods.sessionEnabled = false;
        GooglePauseMethods.toggleTranslation();
        assertTrue(held());
    }

    @Test
    public void googleToggleOffStopsSpeech() {
        GooglePauseMethods.toggleTranslation();
        assertEquals(1, GooglePauseMethods.stopped);
        assertEquals(1, GooglePauseMethods.notifications);
    }

    @Test
    public void googleToggleDoesNotLoadWithoutVideo() {
        GooglePauseMethods.sessionEnabled = false;
        GooglePauseMethods.isLoading = false;
        GooglePauseMethods.currentVideoId = "";
        GooglePauseMethods.segments.clear();
        GooglePauseMethods.toggleTranslation();
        assertEquals(0, GooglePauseMethods.loads);
    }

    @Test
    public void googleToggleDoesNotDuplicateAnActiveLoad() {
        GooglePauseMethods.sessionEnabled = false;
        GooglePauseMethods.segments.clear();
        GooglePauseMethods.toggleTranslation();
        assertEquals(0, GooglePauseMethods.loads);
    }

    @Test
    public void sameVideoHookStillOffersMetadataToAutomaticDispatch() {
        Settings.GOOGLE_VOT_AUTO_TRANSLATE.value = true;
        GooglePauseMethods.newVideoLoaded("a");
        Utils.drain();
        assertEquals(1, GoogleVoiceOverTranslationPatch.starts);
    }

    @Test
    public void nativeCompletionAfterCancellationCannotResume() throws Exception {
        var audio = nativeAudio(new byte[] {1});
        GooglePauseMethods.nativeStartupFinished("prepare", true);
        Utils.drain();
        Utils.background.remove().run();
        GooglePauseMethods.suspendTranslation();
        Utils.drain();
        assertNull(GooglePauseMethods.nativeStartupAudio);
        assertFalse(VideoInformation.playing);
        assertFalse(held());
        assertNull(audio.bytes);
    }

    @Test
    public void emptyRetiredAudioCannotCancelReplacementSynthesis() throws Exception {
        nativeAudio(new byte[0]);
        GooglePauseMethods.nativeStartupFinished("prepare", true);
        Utils.drain();
        Utils.background.remove().run();
        var replacement = nativeAudio(new byte[] {2});
        Utils.drain();
        assertTrue(held());
        assertSame(replacement, GooglePauseMethods.nativeStartupAudio);
        assertTrue(replacement.file.exists());
    }

    @After
    public void after() {
        if (GooglePauseMethods.nativeStartupAudio != null)
            GooglePauseMethods.nativeStartupAudio.file.delete();
    }

    private boolean held() {
        return TranslationPlaybackController.isWaiting(GOOGLE, "a");
    }

    private GooglePauseMethods.NativeStartupAudio prepared() throws Exception {
        return new GooglePauseMethods.NativeStartupAudio(
                "a", "ru", "hello", 0, File.createTempFile("pause-test-", ".wav"));
    }

    @Test
    public void edgeWaitsForMatchingAudio() {
        GooglePauseMethods.checkStartupReady();
        assertTrue(held());
        TtsCache.audio = new byte[] {1};
        GooglePauseMethods.checkStartupReady();
        assertFalse(held());
        assertTrue(VideoInformation.playing);
    }

    @Test
    public void edgeIgnoresWrongVoice() {
        TtsCache.audio = new byte[] {1};
        TtsCache.voice = "old";
        GooglePauseMethods.checkStartupReady();
        assertTrue(held());
    }

    @Test
    public void nativeWaitsForInitializationThenSynthesizesBeforeResuming() {
        GooglePauseMethods.voice = "system";
        GooglePauseMethods.checkStartupReady();
        assertTrue(held());
        assertEquals(0, GooglePauseMethods.tts.syntheses);
        GooglePauseMethods.ttsReady = true;
        GooglePauseMethods.checkStartupReady();
        assertTrue(held());
        assertEquals(1, GooglePauseMethods.tts.syntheses);
        assertEquals(1f, GooglePauseMethods.tts.rate, 0);
        assertNotNull(GooglePauseMethods.nativeStartupAudio);
        GooglePauseMethods.checkStartupReady();
        assertEquals(1, GooglePauseMethods.tts.syntheses);
        assertTrue(held());
        GooglePauseMethods.nativeStartupAudio.bytes = new byte[] {1};
        GooglePauseMethods.checkStartupReady();
        assertFalse(held());
        assertTrue(VideoInformation.playing);
    }

    @Test
    public void nativeFailureLeavesManualPlayAvailable() {
        GooglePauseMethods.voice = "system";
        GooglePauseMethods.ttsReady = true;
        GooglePauseMethods.tts.result = -1;
        GooglePauseMethods.checkStartupReady();
        assertFalse(held());
        assertFalse(VideoInformation.playing);
        assertNull(GooglePauseMethods.nativeStartupAudio);
        assertTrue(
                TranslationPlaybackController.overridePlayWhenReady(VideoInformation.player, true));
    }

    @Test
    public void nativeExceptionLeavesManualPlayAvailable() {
        GooglePauseMethods.voice = "system";
        GooglePauseMethods.ttsReady = true;
        GooglePauseMethods.tts.throwsError = true;
        GooglePauseMethods.checkStartupReady();
        assertFalse(held());
        assertNull(GooglePauseMethods.nativeStartupAudio);
    }

    @Test
    public void unavailableVoiceReleasesHold() {
        GooglePauseMethods.voice = null;
        GooglePauseMethods.checkStartupReady();
        assertFalse(held());
        assertFalse(VideoInformation.playing);
    }

    @Test
    public void disabledSessionDoesNotRelease() {
        GooglePauseMethods.sessionEnabled = false;
        TtsCache.audio = new byte[] {1};
        GooglePauseMethods.checkStartupReady();
        assertTrue(held());
        assertEquals(-1, TtsPrefetcher.time);
    }

    @Test
    public void wrongProviderCannotRelease() {
        Settings.VOT_ENABLED.value = true;
        Settings.VOT_PAUSE_VIDEO_WHILE_PREPARING_TRANSLATION.value = true;
        TranslationPlaybackController.select(YANDEX, "a");
        TtsCache.audio = new byte[] {1};
        GooglePauseMethods.checkStartupReady();
        assertTrue(TranslationPlaybackController.isWaiting(YANDEX, "a"));
        assertEquals(-1, TtsPrefetcher.time);
    }

    @Test
    public void untranslatedSegmentWaitsWhileLoading() {
        GooglePauseMethods.segments.get(0).lang = "en";
        TtsCache.audio = new byte[] {1};
        GooglePauseMethods.checkStartupReady();
        assertTrue(held());
        GooglePauseMethods.isLoading = false;
        GooglePauseMethods.checkStartupReady();
        assertFalse(held());
        assertFalse(VideoInformation.playing);
    }

    @Test
    public void pendingTranslationCannotUseStaleCachedAudio() {
        TranscriptTranslator.awaiting = true;
        TtsCache.audio = new byte[] {1};
        GooglePauseMethods.checkStartupReady();
        assertTrue(held());
        GooglePauseMethods.isLoading = false;
        GooglePauseMethods.checkStartupReady();
        assertFalse(held());
        assertFalse(VideoInformation.playing);
    }

    @Test
    public void noFutureSegmentsWaitsUntilLoadingEnds() {
        VideoInformation.time = 10000;
        TtsCache.audio = new byte[] {1};
        GooglePauseMethods.checkStartupReady();
        assertTrue(held());
        GooglePauseMethods.isLoading = false;
        GooglePauseMethods.checkStartupReady();
        assertFalse(held());
    }

    @Test
    public void currentPositionChoosesTheMatchingSegment() {
        TranscriptSegment second = new TranscriptSegment();
        second.text = "second";
        second.startMs = 10000;
        second.playbackEndMs = 20000;
        GooglePauseMethods.segments.add(second);
        VideoInformation.time = 10000;
        TtsCache.index = 1;
        TtsCache.text = "second";
        TtsCache.audio = new byte[] {1};
        GooglePauseMethods.checkStartupReady();
        assertFalse(held());
        assertEquals(10000, TtsPrefetcher.time);
    }

    @Test
    public void negativePositionIsClamped() {
        VideoInformation.time = -500;
        GooglePauseMethods.checkStartupReady();
        assertEquals(0, TtsPrefetcher.time);
    }

    @Test
    public void expiredNativeGenerationCannotRelease() throws Exception {
        GooglePauseMethods.voice = "system";
        GooglePauseMethods.ttsReady = true;
        GooglePauseMethods.NativeStartupAudio old = prepared();
        old.bytes = new byte[] {1};
        GooglePauseMethods.nativeStartupAudio = old;
        GooglePauseMethods.transcriptGeneration++;
        GooglePauseMethods.checkStartupReady();
        assertTrue(held());
        assertFalse(old.file.exists());
        assertNotSame(old, GooglePauseMethods.nativeStartupAudio);
    }

    @Test
    public void wrongNativeTextCannotRelease() throws Exception {
        GooglePauseMethods.voice = "system";
        GooglePauseMethods.ttsReady = true;
        GooglePauseMethods.nativeStartupAudio = prepared();
        GooglePauseMethods.nativeStartupAudio.bytes = new byte[] {1};
        GooglePauseMethods.segments.get(0).text = "changed";
        GooglePauseMethods.checkStartupReady();
        assertTrue(held());
        assertEquals(1, GooglePauseMethods.tts.syntheses);
    }

    @Test
    public void voiceSwitchRearmsAtCurrentPositionWithoutReopening() throws Exception {
        TtsCache.audio = new byte[] {1};
        GooglePauseMethods.checkStartupReady();
        assertTrue(VideoInformation.playing);
        GooglePauseMethods.nativeStartupAudio = prepared();
        GooglePauseMethods.voice = "system";
        GooglePauseMethods.ttsReady = true;
        VideoInformation.time = 4500;
        GooglePauseMethods.onVoiceChanged();
        assertTrue(held());
        assertFalse(VideoInformation.playing);
        assertEquals(1, GooglePauseMethods.stopped);
        assertEquals(-1, GooglePauseMethods.lastSpokenIndex);
        assertEquals(1, GooglePauseMethods.resets);
        assertEquals(1, TtsPrefetcher.updates);
        assertEquals(4500, TtsPrefetcher.time);
        assertEquals(1, GooglePauseMethods.tts.syntheses);
        assertTrue(held());
        GooglePauseMethods.nativeStartupAudio.bytes = new byte[] {1};
        GooglePauseMethods.checkStartupReady();
        assertTrue(VideoInformation.playing);
    }

    @Test
    public void switchToCachedEdgePreservesManualPause() {
        TranslationPlaybackController.failed(GOOGLE, "a");
        VideoInformation.playing = false;
        TtsCache.audio = new byte[] {1};
        GooglePauseMethods.onVoiceChanged();
        assertFalse(held());
        assertFalse(VideoInformation.playing);
    }

    @Test
    public void changingVoiceWhileDisabledDoesNotStartTranslation() {
        GooglePauseMethods.sessionEnabled = false;
        GooglePauseMethods.onVoiceChanged();
        assertEquals(0, TtsPrefetcher.updates);
        assertEquals(0, GooglePauseMethods.loads);
    }

    @Test
    public void voiceChangeWithoutVideoDoesNotSelectEmptyOwner() {
        GooglePauseMethods.currentVideoId = "";
        GooglePauseMethods.onVoiceChanged();
        assertEquals(0, TtsPrefetcher.updates);
        assertTrue(held());
    }

    @Test
    public void reloadInvalidatesOldTranscriptAndNativeAudio() throws Exception {
        GooglePauseMethods.isLoading = false;
        GooglePauseMethods.nativeStartupAudio = prepared();
        File f = GooglePauseMethods.nativeStartupAudio.file;
        long generation = GooglePauseMethods.transcriptGeneration;
        GooglePauseMethods.reloadTranscript();
        assertEquals(generation + 1, GooglePauseMethods.transcriptGeneration);
        assertNull(GooglePauseMethods.nativeStartupAudio);
        assertFalse(f.exists());
        assertTrue(GooglePauseMethods.segments.isEmpty());
        assertEquals(-1, GooglePauseMethods.lastSpokenIndex);
        assertEquals(1, TtsPrefetcher.clears);
        assertEquals(1, TranscriptTranslator.aborted);
        assertEquals(1, GooglePauseMethods.loads);
        assertTrue(held());
    }

    @Test
    public void reloadDefersReplacementWhileOldFetchRetires() {
        GooglePauseMethods.isLoading = true;
        GooglePauseMethods.reloadTranscript();
        assertEquals(0, GooglePauseMethods.loads);
        assertEquals(2, GooglePauseMethods.transcriptGeneration);
        assertTrue(held());
    }

    @Test
    public void disabledReloadDoesNotFetchOrClaimPause() {
        GooglePauseMethods.sessionEnabled = false;
        GooglePauseMethods.isLoading = false;
        TranslationPlaybackController.failed(GOOGLE, "a");
        GooglePauseMethods.reloadTranscript();
        assertEquals(0, GooglePauseMethods.loads);
        assertFalse(held());
    }

    @Test
    public void reloadWithoutVideoDoesNothing() {
        GooglePauseMethods.currentVideoId = "";
        GooglePauseMethods.reloadTranscript();
        assertEquals(1, GooglePauseMethods.transcriptGeneration);
        assertEquals(0, GooglePauseMethods.stopped);
    }

    @Test
    public void pauseDoesNotCancelPendingNativeSynthesis() throws Exception {
        GooglePauseMethods.nativeStartupAudio = prepared();
        GooglePauseMethods.tts.speaking = true;
        GooglePauseMethods.onPlaybackPaused();
        assertEquals(0, GooglePauseMethods.stopped);
        assertEquals(1, GooglePauseMethods.ttsEngine.pauses);
    }

    @Test
    public void pauseStopsSpeakingNativeAudioAfterPreparation() throws Exception {
        GooglePauseMethods.nativeStartupAudio = prepared();
        GooglePauseMethods.nativeStartupAudio.bytes = new byte[] {1};
        GooglePauseMethods.tts.speaking = true;
        GooglePauseMethods.onPlaybackPaused();
        assertEquals(1, GooglePauseMethods.stopped);
    }

    @Test
    public void pauseStopsNativeSpeechWithoutStartupFile() {
        GooglePauseMethods.tts.speaking = true;
        GooglePauseMethods.onPlaybackPaused();
        assertEquals(1, GooglePauseMethods.stopped);
    }

    @Test
    public void pausePausesEdgeWhenNativeIsAbsent() {
        GooglePauseMethods.tts = null;
        GooglePauseMethods.onPlaybackPaused();
        assertEquals(1, GooglePauseMethods.ttsEngine.pauses);
    }

    @Test
    public void suspendReleasesHoldAndRetiresPreparedAudio() throws Exception {
        GooglePauseMethods.nativeStartupAudio = prepared();
        GooglePauseMethods.suspendTranslation();
        assertFalse(GooglePauseMethods.sessionEnabled);
        assertFalse(Settings.GOOGLE_VOT_SESSION_ENABLED.get());
        assertEquals(2, GooglePauseMethods.transcriptGeneration);
        assertNull(GooglePauseMethods.nativeStartupAudio);
        assertEquals(1, TranscriptTranslator.aborted);
        assertEquals(1, TtsPrefetcher.clears);
        assertFalse(held());
    }

    @Test
    public void automaticStartPreparesExistingTranscript() {
        GooglePauseMethods.sessionEnabled = false;
        TtsCache.audio = new byte[] {1};
        GooglePauseMethods.startAutomaticTranslation();
        assertTrue(GooglePauseMethods.sessionEnabled);
        assertTrue(Settings.GOOGLE_VOT_SESSION_ENABLED.get());
        assertEquals(0, GooglePauseMethods.loads);
        assertFalse(held());
        assertTrue(VideoInformation.playing);
    }

    @Test
    public void automaticStartLoadsMissingTranscript() {
        GooglePauseMethods.segments.clear();
        GooglePauseMethods.isLoading = false;
        GooglePauseMethods.startAutomaticTranslation();
        assertEquals(1, GooglePauseMethods.loads);
        assertTrue(held());
    }

    @Test
    public void automaticStartDoesNotDuplicateFetch() {
        GooglePauseMethods.segments.clear();
        GooglePauseMethods.startAutomaticTranslation();
        assertEquals(0, GooglePauseMethods.loads);
    }

    @Test
    public void disabledGoogleDoesNotStart() {
        Settings.GOOGLE_VOT_ENABLED.value = false;
        GooglePauseMethods.sessionEnabled = false;
        GooglePauseMethods.startAutomaticTranslation();
        assertFalse(GooglePauseMethods.sessionEnabled);
        assertEquals(0, GooglePauseMethods.notifications);
    }

    @Test
    public void preparedNativeAudioMustMatchLanguageAndSegment() throws Exception {
        GooglePauseMethods.voice = "system";
        GooglePauseMethods.ttsReady = true;
        GooglePauseMethods.nativeStartupAudio =
                new GooglePauseMethods.NativeStartupAudio(
                        "a", "en", "hello", 0, File.createTempFile("pause-test-", ".wav"));
        GooglePauseMethods.nativeStartupAudio.bytes = new byte[] {1};
        GooglePauseMethods.checkStartupReady();
        assertTrue(held());
        assertEquals(1, GooglePauseMethods.tts.syntheses);
        GooglePauseMethods.nativeStartupAudio.file.delete();
        GooglePauseMethods.nativeStartupAudio =
                new GooglePauseMethods.NativeStartupAudio(
                        "a", "ru", "hello", 1, File.createTempFile("pause-test-", ".wav"));
        GooglePauseMethods.nativeStartupAudio.bytes = new byte[] {1};
        GooglePauseMethods.checkStartupReady();
        assertTrue(held());
        assertEquals(2, GooglePauseMethods.tts.syntheses);
    }

    @Test
    public void nativePreparationInitializesMissingEngineAndLanguage() {
        GooglePauseMethods.voice = "system";
        GooglePauseMethods.tts = null;
        GooglePauseMethods.ttsReady = true;
        GooglePauseMethods.checkStartupReady();
        assertTrue(held());
        assertNotNull(GooglePauseMethods.tts);
        assertEquals(1, GooglePauseMethods.languages);
    }

    @Test
    public void idleNativeEngineIsNotStoppedByPause() {
        GooglePauseMethods.tts.speaking = false;
        GooglePauseMethods.onPlaybackPaused();
        assertEquals(0, GooglePauseMethods.stopped);
        assertEquals(1, GooglePauseMethods.ttsEngine.pauses);
    }

    @Test
    public void reloadingPlayingVideoPausesForReplacementAudio() {
        TranslationPlaybackController.ready(GOOGLE, "a");
        GooglePauseMethods.isLoading = false;
        GooglePauseMethods.reloadTranscript();
        assertTrue(held());
        assertFalse(VideoInformation.playing);
        assertEquals(1, GooglePauseMethods.stopped);
        assertEquals(1, GooglePauseMethods.cacheUpdates);
        assertEquals(1, GooglePauseMethods.notifications);
    }

    @Test
    public void automaticStartRearmsAnAlreadyReleasedVideo() {
        TranslationPlaybackController.ready(GOOGLE, "a");
        GooglePauseMethods.isLoading = false;
        GooglePauseMethods.startAutomaticTranslation();
        assertTrue(held());
        assertFalse(VideoInformation.playing);
        assertEquals(0, GooglePauseMethods.loads);
        assertEquals(1, GooglePauseMethods.notifications);
    }

    @Test
    public void suspensionStopsAndNotifies() {
        Settings.GOOGLE_VOT_SESSION_ENABLED.value = true;
        GooglePauseMethods.suspendTranslation();
        assertFalse(Settings.GOOGLE_VOT_SESSION_ENABLED.get());
        assertEquals(1, GooglePauseMethods.stopped);
        assertEquals(1, GooglePauseMethods.notifications);
    }

    @Test
    public void voiceSwitchNotifiesState() {
        GooglePauseMethods.onVoiceChanged();
        assertEquals(1, GooglePauseMethods.notifications);
    }

    @Test
    public void pauseEntryPointsRequireMainThread() {
        Utils.mainThread = false;
        try {
            GooglePauseMethods.checkStartupReady();
            fail();
        } catch (IllegalStateException expected) {
        }
        try {
            GooglePauseMethods.onPlaybackPaused();
            fail();
        } catch (IllegalStateException expected) {
        }
        try {
            GooglePauseMethods.onVoiceChanged();
            fail();
        } catch (IllegalStateException expected) {
        }
        try {
            GooglePauseMethods.reloadTranscript();
            fail();
        } catch (IllegalStateException expected) {
        } finally {
            Utils.mainThread = true;
        }
    }

    @Test
    public void voiceSwitchRetargetsPrefetchWhenPauseIsDisabled() {
        Settings.GOOGLE_VOT_PAUSE_WHILE_PREPARING.value = false;
        VideoInformation.time = 6200;
        GooglePauseMethods.onVoiceChanged();
        assertEquals(6200, TtsPrefetcher.time);
        VideoInformation.time = -7;
        GooglePauseMethods.onVoiceChanged();
        assertEquals(0, TtsPrefetcher.time);
    }
}
