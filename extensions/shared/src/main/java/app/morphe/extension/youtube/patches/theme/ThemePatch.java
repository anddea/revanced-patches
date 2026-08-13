package app.morphe.extension.youtube.patches.theme;

import androidx.annotation.Nullable;

import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public final class ThemePatch {
    public enum SplashScreenAnimationStyle {
        // 0 int style exists in target app as a fall through default, but its value is repurposed to be disabled.
        DISABLED(0),
        FPS_60_ONE_SECOND(1),
        FPS_60_TWO_SECOND(2),
        FPS_60_FIVE_SECOND(3),
        FPS_60_BLACK_AND_WHITE(4),
        FPS_30_ONE_SECOND(5),
        FPS_30_TWO_SECOND(6),
        FPS_30_FIVE_SECOND(7),
        FPS_30_BLACK_AND_WHITE(8);
        // There exists a 10th JSON style used as the switch statement default,
        // but visually it is identical to 60fps one second.

        @Nullable
        static SplashScreenAnimationStyle styleFromOrdinal(int style) {
            // Alternatively can return using values()[style]
            for (SplashScreenAnimationStyle value : values()) {
                if (value.style == style) {
                    return value;
                }
            }

            return null;
        }

        final int style;

        SplashScreenAnimationStyle(int style) {
            this.style = style;
        }
    }

    /**
     * Injection point for the splash screen visibility flag.
     */
    public static boolean showSplashScreen(boolean original) {
        return Settings.SPLASH_SCREEN_ANIMATION_STYLE.get() != SplashScreenAnimationStyle.DISABLED && original;
    }

    /**
     * Injection point for the splash screen animation resource selection.
     */
    public static int showSplashScreen(int i, int i2) {
        if (Settings.SPLASH_SCREEN_ANIMATION_STYLE.get() != SplashScreenAnimationStyle.DISABLED || i != i2) {
            return i;
        }
        return i - 1;
    }

    /**
     * Injection point for the splash screen animation style.
     */
    public static int getLoadingScreenType(int original) {
        SplashScreenAnimationStyle style = Settings.SPLASH_SCREEN_ANIMATION_STYLE.get();

        if (style == SplashScreenAnimationStyle.DISABLED) {
            return original;
        }

        final int replacement = style.style;
        if (original != replacement) {
            Logger.printDebug(() -> "Overriding splash screen style from: "
                    + SplashScreenAnimationStyle.styleFromOrdinal(original) + " to: " + style);
        }

        return replacement;
    }
}
