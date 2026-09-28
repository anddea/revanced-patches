package app.morphe.extension.youtube.patches.voiceovertranslation;

import java.lang.ref.WeakReference;
import org.junit.Test;
import static org.junit.Assert.*;
import app.morphe.extension.shared.utils.Utils;

public class NativePlaybackMethodsTest {
    @Test
    public void backendReadRequiresMainThread() {
        Utils.mainThread = false;
        try {
            assertThrows(IllegalStateException.class, NativePlaybackMethods::isPlayerPlaying);
        } finally {
            Utils.mainThread = true;
        }
    }
    @Test
    public void missingOrRetiredBackendCannotUseStalePlayingOverlay() {
        Utils.mainThread = true;
        NativePlaybackMethods.VideoState.current = NativePlaybackMethods.VideoState.PLAYING;
        NativePlaybackMethods.exoPlayerImplRef = new WeakReference<>(null);
        assertFalse(NativePlaybackMethods.isPlayerPlaying());
        var player = new NativePlaybackMethods.ExoPlayerImpl();
        NativePlaybackMethods.exoPlayerImplRef = new WeakReference<>(player);
        player.fail = true;
        assertFalse(NativePlaybackMethods.isPlayerPlaying());
        player.fail = false;
        assertFalse(NativePlaybackMethods.isPlayerPlaying());
        player.playing = true;
        NativePlaybackMethods.VideoState.current = NativePlaybackMethods.VideoState.PAUSED;
        assertTrue(NativePlaybackMethods.isPlayerPlaying());
    }
}
