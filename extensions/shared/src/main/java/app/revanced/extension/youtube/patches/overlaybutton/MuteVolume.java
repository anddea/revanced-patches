package app.revanced.extension.youtube.patches.overlaybutton;

import android.content.Context;
import android.media.AudioManager;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.youtube.settings.Settings;

@SuppressWarnings({"deprecation", "unused"})
public class MuteVolume extends BottomControlButton {
    @Nullable
    private static MuteVolume instance;
    private static AudioManager audioManager;
    private static final int stream = AudioManager.STREAM_MUSIC;

    public MuteVolume(ViewGroup bottomControlsViewGroup) {
        super(
                bottomControlsViewGroup,
                "mute_volume_button",
                Settings.OVERLAY_BUTTON_MUTE_VOLUME,
                view -> {
                    if (instance != null && audioManager != null) {
                        boolean unMuted = !audioManager.isStreamMute(stream);
                        audioManager.setStreamMute(stream, unMuted);
                        instance.changeActivated(unMuted);
                    }
                },
                null
        );
    }

    /**
     * Injection point.
     */
    public static void initialize(View bottomControlsViewGroup) {
        try {
            if (bottomControlsViewGroup instanceof ViewGroup viewGroup) {
                instance = new MuteVolume(viewGroup);
            }
            if (bottomControlsViewGroup.getContext().getSystemService(Context.AUDIO_SERVICE) instanceof AudioManager am) {
                audioManager = am;
            }
        } catch (Exception ex) {
            Logger.printException(() -> "initialize failure", ex);
        }
    }

    /**
     * Injection point.
     */
    public static void changeVisibility(boolean showing, boolean animation) {
        if (instance != null) {
            instance.setVisibility(showing, animation);
            changeActivated(instance);
        }
    }

    public static void changeVisibilityNegatedImmediate() {
        if (instance != null) {
            instance.setVisibilityNegatedImmediate();
            changeActivated(instance);
        }
    }

    private static void changeActivated(MuteVolume instance) {
        if (audioManager != null) {
            boolean muted = audioManager.isStreamMute(stream);
            instance.changeActivated(muted);
        }
    }

}
