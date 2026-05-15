package app.morphe.extension.youtube.patches.shorts;

import android.app.Activity;

import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public class ShortsRepeatStatePatch {

    private enum ShortsLoopBehavior {
        UNKNOWN,
        /**
         * Repeat the same Short forever!
         */
        REPEAT,
        /**
         * Play once, then advance to the next Short.
         */
        SINGLE_PLAY,
        /**
         * Pause playback after 1 play.
         */
        END_SCREEN,
        /**
         * Play once, then advance to the next Short.
         */
        AUTO_ADVANCE,
        AUTO_ADVANCE_POST_TWO_LOOPS,
        AUTO_ADVANCE_POST_THREE_LOOPS,
        AUTO_ADVANCE_POST_FOUR_LOOPS,
        AUTO_ADVANCE_POST_FIVE_LOOPS;

        static void setYTEnumValue(Enum<?> ytBehavior) {
            for (ShortsLoopBehavior behavior : values()) {
                if (ytBehavior.name().endsWith(behavior.name())) {
                    behavior.ytEnumValue = ytBehavior;
                    Logger.printDebug(() -> behavior + " set to YT enum: " + ytBehavior.name());
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
            ShortsLoopBehavior.setYTEnumValue(ytEnum);
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
            boolean autoplay = isAppInBackgroundPiPMode()
                    ? Settings.SHORTS_AUTOPLAY_BACKGROUND.get()
                    : Settings.SHORTS_AUTOPLAY.get();

            ShortsLoopBehavior autoPlayBehavior = ShortsLoopBehavior.AUTO_ADVANCE.ytEnumValue != null
                    ? ShortsLoopBehavior.AUTO_ADVANCE
                    : ShortsLoopBehavior.SINGLE_PLAY;
            Enum<?> overrideBehavior = (autoplay
                    ? autoPlayBehavior
                    : ShortsLoopBehavior.REPEAT).ytEnumValue;

            if (overrideBehavior != null) {
                Logger.printDebug(() -> {
                    String name = original == null ? "unknown (null)" : original.name();
                    return overrideBehavior == original
                            ? "Behavior setting is same as original. Using original: " + name
                            : "Changing Shorts repeat behavior from: " + name + " to: " + overrideBehavior.name();
                });

                return overrideBehavior;
            }

            if (original == null) {
                // Cannot return null, as null is used to indicate the Short was autoplayed.
                Enum<?> unknown = ShortsLoopBehavior.UNKNOWN.ytEnumValue;
                Logger.printDebug(() -> "Original is null, returning: " + unknown.name());
                return unknown;
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
