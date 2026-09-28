package app.morphe.extension.youtube.patches.voiceovertranslation;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.shared.VideoInformation;

public class TtsPlaybackMethodsTest {
    private TtsPlaybackMethods engine;
    private TtsPlaybackMethods.MediaPlayer audio;

    @Before
    public void before() {
        Utils.mainThread = true;
        VideoInformation.playing = false;
        engine = new TtsPlaybackMethods();
        audio = new TtsPlaybackMethods.MediaPlayer();
        engine.currentPlayer = audio;
    }

    @Test
    public void delayedSpeechPreparationWaitsForVideoThenResumesAtRequestedPositionAndRate() {
        engine.startPreparedPlayback(1.5f, 350, true);
        assertFalse(audio.playing);
        assertEquals(0, audio.starts);
        assertEquals(0, audio.rateChanges);
        assertEquals(0, audio.pauses);
        assertEquals(350, audio.position);
        VideoInformation.playing = true;
        engine.resume();
        assertTrue(audio.playing);
        assertEquals(1, audio.starts);
        assertEquals(1.5f, audio.rate, 0);
        engine.resume();
        assertEquals(1, audio.starts);
    }

    @Test
    public void rateChangeCannotRestartPausedSpeechAndIsAppliedWhenVideoResumes() {
        VideoInformation.playing = true;
        engine.startPreparedPlayback(1, 0, true);
        engine.pause();
        VideoInformation.playing = false;
        engine.setPlaybackRate(2);
        engine.resume();
        assertFalse(audio.playing);
        assertEquals(1, audio.rateChanges);
        VideoInformation.playing = true;
        engine.resume();
        assertTrue(audio.playing);
        assertEquals(2, audio.rate, 0);
        assertEquals(2, audio.starts);
        engine.pause();
        engine.setPlaybackRate(1);
        engine.resume();
        assertEquals(1, audio.rate, 0);
    }

    @Test
    public void stalePlayingEventCannotResumeSpeechAgainstPausedBackend() {
        VideoInformation.playing = true;
        engine.startPreparedPlayback(1, 0, true);
        VideoInformation.playing = false;
        engine.resume();
        assertFalse(audio.playing);
        assertEquals(1, audio.starts);
    }

    @Test
    public void speedCallbackStopsSpeechWhenBackendHasPaused() {
        VideoInformation.playing = true;
        engine.startPreparedPlayback(1, 0, true);
        VideoInformation.playing = false;
        engine.setPlaybackRate(2);
        assertFalse(audio.playing);
        assertEquals(1, audio.rateChanges);
    }

    @Test
    public void speedChangeWaitsForExplicitAudioResumeEvenWhenVideoHasAlreadyResumed() {
        VideoInformation.playing = true;
        engine.startPreparedPlayback(1, 0, true);
        engine.pause();
        engine.setPlaybackRate(2);
        assertFalse(audio.playing);
        assertEquals(1, audio.rateChanges);
        engine.resume();
        assertTrue(audio.playing);
        assertEquals(2, audio.rate, 0);
    }

    @Test
    public void playingSpeechChangesSpeedNormally() {
        VideoInformation.playing = true;
        engine.startPreparedPlayback(1, 0, true);
        engine.setPlaybackRate(0.75f);
        assertTrue(audio.playing);
        assertEquals(0.75f, audio.rate, 0);
    }

    @Test
    public void voicePreviewStillPlaysWithoutVideo() {
        engine.startPreparedPlayback(1.25f, 0, false);
        assertTrue(audio.playing);
        assertEquals(1.25f, audio.rate, 0);
        engine.setPlaybackRate(1.5f);
        assertTrue(audio.playing);
        assertEquals(1.5f, audio.rate, 0);
    }

    @Test
    public void controlsBeforeAudioExistsAreHarmless() {
        engine.pause();
        assertEquals(0, audio.pauses);
        engine.currentPlayer = null;
        engine.pause();
        engine.resume();
        engine.setPlaybackRate(2);
        assertEquals(0, audio.starts);
    }

    @Test
    public void playbackControlsTolerateReleasedAudio() {
        audio.fail = true;
        engine.pause();
        engine.resume();
        engine.setPlaybackRate(2);
        assertEquals(0, audio.starts);
    }

    @Test
    public void preparationFailureReachesTheOwnerThatReleasesTheWaitingSynthesisThread() {
        audio.fail = true;
        VideoInformation.playing = true;
        assertThrows(IllegalStateException.class,
                () -> engine.startPreparedPlayback(1, 0, true));
        assertEquals(0, audio.starts);
    }

    @Test
    public void preparationSeeksOnlyWhenPositionIsPositive() {
        engine.startPreparedPlayback(1, 0, true);
        assertEquals(0, audio.seeks);
        engine.startPreparedPlayback(1, 1, true);
        assertEquals(1, audio.seeks);
        assertEquals(1, audio.position);
        assertEquals(0, audio.starts);
    }

    @Test
    public void audioControlsRequireMainThread() {
        Utils.mainThread = false;
        try {
            assertThrows(IllegalStateException.class, () -> engine.pause());
            assertThrows(IllegalStateException.class, () -> engine.resume());
            assertThrows(IllegalStateException.class, () -> engine.setPlaybackRate(2));
        } finally {
            Utils.mainThread = true;
        }
    }
}
