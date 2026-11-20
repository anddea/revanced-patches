package app.morphe.extension.youtube.patches.voiceovertranslation;

import static org.junit.Assert.*;
import static app.morphe.extension.youtube.patches.voiceovertranslation.YandexAudioMethods.*;
import app.morphe.extension.youtube.settings.Settings;
import org.junit.*;

public class YandexAudioMethodsTest {
    MediaPlayer audio;
    @Before public void before() {
        Settings.VOT_ENABLED.value = true;
        mediaPlayer.set(audio = new MediaPlayer());
        mainHandler = new Handler();
        isPaused = shortsPlaybackPaused = RootView.shorts = false;
        lastAppliedPlaybackSpeed = 1;
        lastVideoTimeMs = -1;
        VideoInformation.time = 0;
        VideoInformation.length = 100000;
        VideoInformation.speed = VideoInformation.cachedSpeed = 1;
        VideoInformation.playing = true;
        deleted = 0;
    }
    @Test public void stoppedVideoCannotKeepTranslationAliveThroughTimeTicks() {
        VideoInformation.playing = false;
        setVideoTime(100000);
        mainHandler.drain();
        assertFalse(audio.playing);
        assertEquals(0, audio.rateChanges);
        assertEquals(0, audio.seeks);
        assertTrue(isPaused);
    }
    @Test public void endPositionCannotResumeBeforeBackendReportsEnded() {
        VideoInformation.time = 100000;
        audio.playing = false;
        isPaused = true;
        resumeAudio(-1);
        assertFalse(audio.playing);
        assertEquals(0, audio.starts);
    }
    @Test public void unchangedRateCannotRestartAudioThatFinishesBetweenChecks() {
        audio.finishBeforeRateChange = true;
        setVideoTime(1000);
        mainHandler.drain();
        assertEquals(0, audio.rateChanges);
        assertEquals(0, audio.starts);
    }
    @Test public void queuedTickCannotSeekAReplacementPlayer() {
        setVideoTime(80000);
        MediaPlayer replacement = new MediaPlayer();
        mediaPlayer.set(replacement);
        mainHandler.drain();
        assertEquals(0, replacement.seeks);
        assertEquals(0, replacement.rateChanges);
    }
    @Test public void completedShorterAudioCannotRestartAtOrBeyondItsEnd() {
        audio.duration = 90000;
        audio.playing = false;
        isPaused = true;
        resumeAudio(90000);
        resumeAudio(90001);
        assertFalse(audio.playing);
        assertEquals(0, audio.starts);
    }
    @Test public void realSpeedChangesAndBackwardSeeksStillWork() {
        VideoInformation.speed = 1.5f;
        setVideoTime(5000);
        mainHandler.drain();
        assertEquals(1.5f, audio.speed, 0);
        assertEquals(1, audio.rateChanges);
        lastVideoTimeMs = 10000;
        setVideoTime(1000);
        mainHandler.drain();
        assertEquals(1000, audio.position);
        assertEquals(1, audio.seeks);
    }
    @Test public void completionStopsAnImplicitRestartAndReplayStillWorks() {
        audio.position = 0;
        audio.playing = true; // A speed update won the race with the completion callback.
        onAudioCompleted(audio);
        assertFalse(audio.playing);
        assertTrue(isPaused);
        assertEquals(1, deleted);
        VideoInformation.time = 100000;
        resumeAudio(-1);
        assertFalse(audio.playing);
        VideoInformation.time = 0;
        resumeAudio(-1);
        assertTrue(audio.playing);
        assertEquals(0, audio.position);
        assertFalse(isPaused);
    }
    @Test public void directAndDownloadedAudioBothRegisterCompletionProtection() {
        for (boolean file : new boolean[] {false, true}) {
            before();
            if (file) fileCompletionSetup(audio); else directCompletionSetup(audio);
            assertNotNull(audio.completed);
            audio.completed.accept(audio);
            assertFalse(audio.playing);
            assertTrue(isPaused);
            assertEquals(1, deleted);
        }
    }
    @Test public void naturalCompletionCanBeFollowedByABackwardSeek() {
        audio.playing = false;
        audio.complete = true;
        audio.position = 100000;
        onAudioCompleted(audio);
        resumeAudio(99999);
        assertTrue(audio.playing);
        assertEquals(99999, audio.position);
    }
    @Test public void obsoleteCompletionCannotPauseOrCleanUpNewAudio() {
        onAudioCompleted(new MediaPlayer());
        assertTrue(audio.playing);
        assertFalse(isPaused);
        assertEquals(0, deleted);
    }
    @Test public void delayedResumeCannotSeekAReplacementPlayer() {
        isPaused = true;
        audio.playing = false;
        setVideoTime(80000);
        MediaPlayer replacement = new MediaPlayer();
        mediaPlayer.set(replacement);
        mainHandler.delayed.keySet().forEach(Runnable::run);
        assertEquals(0, replacement.seeks);
        assertEquals(0, replacement.starts);
    }
    @Test public void pausedTickResumesOnlyItsOwnAudio() {
        isPaused = true;
        audio.playing = false;
        setVideoTime(1234);
        assertTrue(mainHandler.delayed.containsValue(80L));
        assertEquals(Long.valueOf(1500), mainHandler.delayed.get(pauseCheckRunnable));
        mainHandler.delayed.keySet().forEach(Runnable::run);
        assertTrue(audio.playing);
        assertEquals(1234, audio.position);
    }
    @Test public void negativePositionDoesNotRestartAndUnknownVideoLengthCanPlay() {
        isPaused = true;
        audio.playing = false;
        VideoInformation.time = -1;
        resumeAudio(-1);
        assertFalse(audio.playing);
        VideoInformation.length = 0;
        VideoInformation.time = 0;
        resumeAudio(-1);
        assertTrue(audio.playing);
    }
    @Test public void inactiveAndMissingAudioDoNotScheduleWork() {
        Settings.VOT_ENABLED.value = false;
        setVideoTime(0);
        assertTrue(mainHandler.queue.isEmpty());
        assertTrue(mainHandler.delayed.isEmpty());
        Settings.VOT_ENABLED.value = true;
        mediaPlayer.set(null);
        setVideoTime(0);
        assertTrue(mainHandler.queue.isEmpty());
        assertTrue(mainHandler.delayed.isEmpty());
        pauseAudio();
        resumeAudio(0);
        applyPlaybackSpeedToPlayer(null);
    }
    @Test public void completedAudioDoesNotReceiveQueuedSpeedUpdates() {
        audio.finishAfterRead = true;
        VideoInformation.speed = 2;
        setVideoTime(99999);
        mainHandler.drain();
        assertEquals(0, audio.rateChanges);
        audio.finishAfterRead = false;
        audio.playing = false;
        setVideoTime(99999);
        assertTrue(mainHandler.queue.isEmpty());
    }
    @Test public void regularTicksDoNotQueueResumeAndResetPauseDeadline() {
        setVideoTime(100);
        assertEquals(1, mainHandler.delayed.size());
        assertEquals(1, mainHandler.removed);
        mainHandler.drain();
        assertEquals(100, lastVideoTimeMs);
        assertEquals(0, audio.seeks);
    }
    @Test public void resumeRequiresPausedAudioAndAnActuallyPlayingVideo() {
        resumeAudio(100);
        assertEquals(0, audio.seeks);
        isPaused = true;
        audio.playing = false;
        VideoInformation.playing = false;
        resumeAudio(100);
        assertFalse(audio.playing);
        VideoInformation.playing = true;
        VideoInformation.speed = 2;
        VideoInformation.time = 600;
        resumeAudio(0);
        assertEquals(0, audio.position);
        assertEquals(2, audio.speed, 0);
        assertFalse(isPaused);
    }
    @Test public void videoEndGateAlsoProtectsLongerTranslationAudio() {
        audio.duration = 200000;
        VideoInformation.time = 100000;
        assertFalse(shouldPlayTranslationAudio());
        isPaused = true;
        audio.playing = false;
        resumeAudio(-1);
        assertFalse(audio.playing);
        VideoInformation.time = 99999;
        assertTrue(shouldPlayTranslationAudio());
    }
    @Test public void shortsUseTheirOwnPauseSignal() {
        RootView.shorts = true;
        VideoInformation.playing = false;
        assertTrue(shouldPlayTranslationAudio());
        shortsPlaybackPaused = true;
        assertFalse(shouldPlayTranslationAudio());
    }
    @Test public void redundantPauseAndReleasedAudioAreHarmless() {
        audio.playing = false;
        pauseAudio();
        assertEquals(0, audio.pauses);
        assertFalse(isPaused);
        audio.fail = true;
        pauseAudio();
        assertFalse(isPaused);
        audio.fail = false;
        isPaused = true;
        audio.failRate = true;
        VideoInformation.speed = 2;
        resumeAudio(0);
        assertEquals(1, audio.starts);
        assertEquals(1, lastAppliedPlaybackSpeed, 0);
        audio.failPosition = true;
        setVideoTime(1000);
        mainHandler.drain();
    }
    @Test public void speedFallbackAndClampsPreserveSupportedRates() {
        float[][] cases = {{-1, 1.5f, 1.5f}, {0, 0.75f, 0.75f},
                {-1, -1, 1}, {0, 0, 1}, {0.1f, 1, 0.25f}, {0.25f, 1, 0.25f},
                {2.5f, 1, 2.5f}, {3, 1, 2.5f}, {0.5f, 2, 0.5f}, {1, 2, 1}};
        for (float[] c : cases) {
            before();
            lastAppliedPlaybackSpeed = 1.25f;
            VideoInformation.speed = c[0]; VideoInformation.cachedSpeed = c[1];
            applyPlaybackSpeedToPlayer(audio);
            assertEquals(c[2], audio.speed, 0);
            assertEquals(c[2], lastAppliedPlaybackSpeed, 0);
            assertEquals(c[0] > 0 ? c[0] : c[1], VideoInformation.cachedSpeed, 0);
            applyPlaybackSpeedToPlayer(audio);
            assertEquals(1, audio.rateChanges);
        }
    }
    @Test public void driftAndSeekBoundariesPreserveNormalPlayback() {
        long[][] cases = {{-1, 1000, 1000, 0}, {0, 3001, 3001, 1},
                {10000, 9500, 9500, 0}, {10000, 9499, 9499, 1},
                {10000, 13000, 13000, 0}, {10000, 13001, 13001, 1},
                {-1, 1000, 21000, 0}, {-1, 1000, 21001, 1},
                {-1, 21001, 1000, 1}, {-1, 20001, 20000, 0}};
        for (long[] c : cases) {
            before();
            lastVideoTimeMs = c[0]; audio.position = (int)c[2];
            setVideoTime(c[1]); mainHandler.drain();
            assertEquals(java.util.Arrays.toString(c), c[3], audio.seeks);
            assertEquals(c[1], lastVideoTimeMs);
            assertEquals(c[3] == 1 ? c[1] : c[2], audio.position);
        }
    }
}
