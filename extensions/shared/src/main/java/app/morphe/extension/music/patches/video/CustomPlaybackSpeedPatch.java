package app.morphe.extension.music.patches.video;

import static app.morphe.extension.shared.utils.StringRef.str;

import androidx.annotation.NonNull;

import java.util.Arrays;

import app.morphe.extension.music.settings.Settings;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;

@SuppressWarnings("unused")
public class CustomPlaybackSpeedPatch {
    /**
     * Maximum playback speed, exclusive value.  Custom speeds must be less than this value.
     */
    private static final float MAXIMUM_PLAYBACK_SPEED = 5;

    /**
     * Custom playback speeds.
     */
    private static float[] customPlaybackSpeeds;

    static {
        loadCustomSpeeds();
    }

    /**
     * Injection point.
     */
    public static float[] getArray(float[] original) {
        return userChangedCustomPlaybackSpeed() ? customPlaybackSpeeds : original;
    }

    /**
     * Injection point.
     */
    public static int getLength(int original) {
        return userChangedCustomPlaybackSpeed() ? customPlaybackSpeeds.length : original;
    }

    /**
     * Injection point.
     */
    public static int getSize(int original) {
        return userChangedCustomPlaybackSpeed() ? 0 : original;
    }

    private static void resetCustomSpeeds(@NonNull String toastMessage) {
        Utils.showToastLong(toastMessage);
        Utils.showToastShort(str("revanced_reset_to_default_toast"));
        Settings.CUSTOM_PLAYBACK_SPEEDS.resetToDefault();
    }

    public static void loadCustomSpeeds() {
        try {
            String[] speedStrings = Settings.CUSTOM_PLAYBACK_SPEEDS.get().split("\\s+");
            Arrays.sort(speedStrings);
            if (speedStrings.length == 0) {
                throw new IllegalArgumentException();
            }
            customPlaybackSpeeds = new float[speedStrings.length];
            for (int i = 0, length = speedStrings.length; i < length; i++) {
                final float speed = Float.parseFloat(speedStrings[i]);
                if (speed <= 0 || arrayContains(customPlaybackSpeeds, speed)) {
                    throw new IllegalArgumentException();
                }
                if (speed > MAXIMUM_PLAYBACK_SPEED) {
                    resetCustomSpeeds(str("revanced_custom_playback_speeds_invalid", MAXIMUM_PLAYBACK_SPEED + ""));
                    loadCustomSpeeds();
                    return;
                }
                customPlaybackSpeeds[i] = speed;
            }
        } catch (Exception ex) {
            Logger.printInfo(() -> "parse error", ex);
            resetCustomSpeeds(str("revanced_custom_playback_speeds_parse_exception"));
            loadCustomSpeeds();
        }
    }

    private static boolean userChangedCustomPlaybackSpeed() {
        return !Settings.CUSTOM_PLAYBACK_SPEEDS.isSetToDefault() && customPlaybackSpeeds != null;
    }

    private static boolean arrayContains(float[] array, float value) {
        for (float arrayValue : array) {
            if (arrayValue == value) return true;
        }
        return false;
    }

}
