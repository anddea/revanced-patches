package app.revanced.extension.youtube.patches.shorts;

import android.app.Activity;

import androidx.annotation.Nullable;

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
                String ytName = ytBehavior.name();
                if (ytName.endsWith(rvBehavior.name())) {
                    if (rvBehavior.ytEnumValue != null) {
                        Logger.printException(() -> "Conflicting behavior names: " + rvBehavior
                                + " ytBehavior: " + ytName);
                    } else {
                        rvBehavior.ytEnumValue = ytBehavior;
                        Logger.printDebug(() -> rvBehavior + " set to YT enum: " + ytName);
                    }
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
    @Nullable
    public static Enum<?> changeShortsRepeatBehavior(@Nullable Enum<?> original) {
        try {
            ShortsLoopBehavior behavior = ExtendedUtils.IS_19_34_OR_GREATER &&
                    isAppInBackgroundPiPMode()
                    ? Settings.CHANGE_SHORTS_BACKGROUND_REPEAT_STATE.get()
                    : Settings.CHANGE_SHORTS_REPEAT_STATE.get();
            Enum<?> overrideBehavior = behavior.ytEnumValue;

            if (behavior != ShortsLoopBehavior.UNKNOWN && overrideBehavior != null) {
                Logger.printDebug(() -> {
                    String name = original == null ? "unknown (null)" : original.name();
                    return overrideBehavior == original
                            ? "Behavior setting is same as original. Using original: " + name
                            : "Changing Shorts repeat behavior from: " + name + " to: " + overrideBehavior.name();
                });

                // For some reason, in YouTube 20.09+, 'UNKNOWN' functions as 'Pause'.
                return ExtendedUtils.IS_20_09_OR_GREATER && behavior == ShortsLoopBehavior.END_SCREEN
                        ? ShortsLoopBehavior.UNKNOWN.ytEnumValue
                        : overrideBehavior;
            }
        } catch (Exception ex) {
            Logger.printException(() -> "changeShortsRepeatBehavior failure", ex);
        }

        return original;
    }

    /**
     * Injection point.
     */
    public static boolean isAutoPlay(@Nullable Enum<?> original) {
        return ShortsLoopBehavior.SINGLE_PLAY.ytEnumValue == original;
    }
}
