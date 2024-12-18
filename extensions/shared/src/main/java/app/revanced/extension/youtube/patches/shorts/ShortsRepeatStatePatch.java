package app.revanced.extension.youtube.patches.shorts;

import android.app.Activity;

import java.lang.ref.WeakReference;
import java.util.Objects;

import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.utils.ExtendedUtils;

@SuppressWarnings("unused")
public class ShortsRepeatStatePatch {

    public enum ShortsLoopBehavior {
        UNKNOWN,
        /**
         * Repeat the same Short forever!
         */
        REPEAT,
        /**
         * Play once, then advanced to the next Short.
         */
        SINGLE_PLAY,
        /**
         * Pause playback after 1 play.
         */
        END_SCREEN;

        static void setYTEnumValue(Enum<?> ytBehavior) {
            for (ShortsLoopBehavior rvBehavior : values()) {
                if (ytBehavior.name().endsWith(rvBehavior.name())) {
                    rvBehavior.ytEnumValue = ytBehavior;

                    Logger.printDebug(() -> rvBehavior + " set to YT enum: " + ytBehavior.name());
                    return;
                }
            }

            Logger.printException(() -> "Unknown Shorts loop behavior: " + ytBehavior.name());
        }

        /**
         * YouTube enum value of the obfuscated enum type.
         */
        private Enum<?> ytEnumValue;
    }

    private static WeakReference<Activity> mainActivityRef = new WeakReference<>(null);


    public static void setMainActivity(Activity activity) {
        mainActivityRef = new WeakReference<>(activity);
    }

    /**
     * @return If the app is currently in background PiP mode.
     */
    private static boolean isAppInBackgroundPiPMode() {
        Activity activity = mainActivityRef.get();
        return activity != null && activity.isInPictureInPictureMode();
    }

    /**
     * Injection point.
     */
    public static void setYTShortsRepeatEnum(Enum<?> ytEnum) {
        try {
            for (Enum<?> ytBehavior : Objects.requireNonNull(ytEnum.getClass().getEnumConstants())) {
                ShortsLoopBehavior.setYTEnumValue(ytBehavior);
            }
        } catch (Exception ex) {
            Logger.printException(() -> "setYTShortsRepeatEnum failure", ex);
        }
    }

    /**
     * Injection point.
     */
    public static Enum<?> changeShortsRepeatBehavior(Enum<?> original) {
        try {
            final ShortsLoopBehavior behavior = ExtendedUtils.IS_19_34_OR_GREATER &&
                    isAppInBackgroundPiPMode()
                    ? Settings.CHANGE_SHORTS_BACKGROUND_REPEAT_STATE.get()
                    : Settings.CHANGE_SHORTS_REPEAT_STATE.get();

            if (behavior != ShortsLoopBehavior.UNKNOWN && behavior.ytEnumValue != null) {
                Logger.printDebug(() -> behavior.ytEnumValue == original
                        ? "Changing Shorts repeat behavior from: " + original.name() + " to: " + behavior.ytEnumValue
                        : "Behavior setting is same as original. Using original: " + original.name()
                );

                return behavior.ytEnumValue;
            }
        } catch (Exception ex) {
            Logger.printException(() -> "changeShortsRepeatState failure", ex);
        }

        return original;
    }
}
