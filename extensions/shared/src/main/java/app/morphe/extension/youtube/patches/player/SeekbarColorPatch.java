package app.morphe.extension.youtube.patches.player;

import static app.morphe.extension.shared.utils.StringRef.str;
import static app.morphe.extension.shared.utils.Utils.clamp;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.AnimatedVectorDrawable;

import java.util.Arrays;
import java.util.Locale;

import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.ResourceUtils;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public class SeekbarColorPatch {

    private static final boolean CUSTOM_SEEKBAR_COLOR_ENABLED =
            Settings.ENABLE_CUSTOM_SEEKBAR_COLOR.get();

    private static final boolean HIDE_SEEKBAR_THUMBNAIL_ENABLED =
            Settings.HIDE_SEEKBAR_THUMBNAIL.get();

    /**
     * Default color of the litho seekbar.
     * Differs slightly from the default custom seekbar color setting.
     */
    private static final int ORIGINAL_SEEKBAR_COLOR = 0xFFFF0000;

    /**
     * Default accent color of the litho seekbar.
     */
    private static final int ORIGINAL_SEEKBAR_COLOR_ACCENT = 0xFFFF2791;

    /**
     * Feed default colors of the gradient seekbar.
     */
    private static final int[] FEED_ORIGINAL_SEEKBAR_GRADIENT_COLORS = {0xFFFF0033, ORIGINAL_SEEKBAR_COLOR_ACCENT};

    /**
     * Feed default positions of the gradient seekbar.
     */
    private static final float[] FEED_ORIGINAL_SEEKBAR_GRADIENT_POSITIONS = {0.8f, 1.0f};

    /**
     * Empty seekbar gradient, if hide seekbar in feed is enabled.
     */
    private static final int[] HIDDEN_SEEKBAR_GRADIENT_COLORS = {0x0, 0x0};

    /**
     * Default YouTube seekbar color brightness.
     */
    private static final float ORIGINAL_SEEKBAR_COLOR_BRIGHTNESS;

    /**
     * If {@link Settings#ENABLE_CUSTOM_SEEKBAR_COLOR} is enabled,
     * this is the color value of {@link Settings#CUSTOM_SEEKBAR_COLOR_PRIMARY}.
     * Otherwise this is {@link #ORIGINAL_SEEKBAR_COLOR}.
     */
    private static final int customSeekbarColor;

    /**
     * If {@link Settings#ENABLE_CUSTOM_SEEKBAR_COLOR} is enabled,
     * this is the color value of {@link Settings#CUSTOM_SEEKBAR_COLOR_ACCENT}.
     * Otherwise this is {@link #ORIGINAL_SEEKBAR_COLOR_ACCENT}.
     */
    private static int customSeekbarColorAccent = ORIGINAL_SEEKBAR_COLOR_ACCENT;

    /**
     * Custom seekbar hue, saturation, and brightness values.
     */
    private static final float[] customSeekbarColorHSV = new float[3];

    /**
     * Custom seekbar color, used for linear gradient replacements.
     */
    private static final int[] customSeekbarColorGradient = new int[2];

    static {
        float[] hsv = new float[3];
        Color.colorToHSV(ORIGINAL_SEEKBAR_COLOR, hsv);
        ORIGINAL_SEEKBAR_COLOR_BRIGHTNESS = hsv[2];

        customSeekbarColor = CUSTOM_SEEKBAR_COLOR_ENABLED
                ? loadCustomSeekbarColor()
                : ORIGINAL_SEEKBAR_COLOR;
    }

    private static int loadCustomSeekbarColor() {
        try {
            final int color = Color.parseColor(Settings.CUSTOM_SEEKBAR_COLOR_PRIMARY.get());
            Color.colorToHSV(color, customSeekbarColorHSV);
            customSeekbarColorAccent = Color.parseColor(Settings.CUSTOM_SEEKBAR_COLOR_ACCENT.get());

            customSeekbarColorGradient[0] = color;
            customSeekbarColorGradient[1] = customSeekbarColorAccent;

            return color;
        } catch (Exception ex) {
            Utils.showToastShort(str("revanced_custom_seekbar_color_invalid_toast"));
            Utils.showToastShort(str("revanced_reset_to_default_toast"));
            Settings.CUSTOM_SEEKBAR_COLOR_PRIMARY.resetToDefault();
            Settings.CUSTOM_SEEKBAR_COLOR_ACCENT.resetToDefault();

            return loadCustomSeekbarColor();
        }
    }

    public static int getSeekbarColor() {
        return customSeekbarColor;
    }

    /**
     * Injection point
     */
    public static boolean useLotteLaunchSplashScreen(boolean original) {
        Logger.printDebug(() -> "useLotteLaunchSplashScreen original: " + original);

        if (CUSTOM_SEEKBAR_COLOR_ENABLED) return false;

        return original;
    }

    private static int colorChannelTo3Bits(int channel8Bits) {
        final float channel3Bits = channel8Bits * 7 / 255f;

        // If a color channel is near zero, then allow rounding up so values between
        // 0x12 and 0x23 will show as 0x24. But always round down when the channel is
        // near full saturation, otherwise rounding to nearest will cause all values
        // between 0xEC and 0xFE to always show as full saturation (0xFF).
        return channel3Bits < 6
                ? Math.round(channel3Bits)
                : (int) channel3Bits;
    }

    @SuppressWarnings("SameParameterValue")
    private static String get9BitStyleIdentifier(int color24Bit) {
        final int r3 = colorChannelTo3Bits(Color.red(color24Bit));
        final int g3 = colorChannelTo3Bits(Color.green(color24Bit));
        final int b3 = colorChannelTo3Bits(Color.blue(color24Bit));

        return String.format(Locale.US, "splash_seekbar_color_style_%d_%d_%d", r3, g3, b3);
    }

    /**
     * Injection point
     */
    public static void setSplashAnimationDrawableTheme(AnimatedVectorDrawable vectorDrawable) {
        // Alternatively a ColorMatrixColorFilter can be used to change the color of the drawable
        // without using any styles, but a color filter cannot selectively change the seekbar
        // while keeping the red YT logo untouched.
        // Even if the seekbar color xml value is changed to a completely different color (such as green),
        // a color filter still cannot be selectively applied when the drawable has more than 1 color.
        try {
            String seekbarStyle = get9BitStyleIdentifier(customSeekbarColor);
            Logger.printDebug(() -> "Using splash seekbar style: " + seekbarStyle);

            final int styleIdentifierDefault = ResourceUtils.getStyleIdentifier(seekbarStyle);
            if (styleIdentifierDefault == 0) {
                throw new RuntimeException("Seekbar style not found: " + seekbarStyle);
            }

            Resources.Theme theme = Utils.getContext().getResources().newTheme();
            theme.applyStyle(styleIdentifierDefault, true);

            vectorDrawable.applyTheme(theme);
        } catch (Exception ex) {
            Logger.printException(() -> "setSplashAnimationDrawableTheme failure", ex);
        }
    }

    /**
     * Injection point
     */
    public static boolean playerSeekbarGradientEnabled(boolean original) {
        return CUSTOM_SEEKBAR_COLOR_ENABLED || original;
    }

    /**
     * Injection point.
     */
    public static boolean showWatchHistoryProgressDrawable(boolean original) {
        return !HIDE_SEEKBAR_THUMBNAIL_ENABLED && original;
    }

    /**
     * Injection point.
     * <p>
     * Overrides all Litho components that use the YouTube seekbar color.
     * Used only for the video thumbnails seekbar.
     * <p>
     * If {@link Settings#HIDE_SEEKBAR_THUMBNAIL} is enabled, this returns a fully transparent color.
     */
    public static int getLithoColor(int colorValue) {
        if (colorValue == ORIGINAL_SEEKBAR_COLOR) {
            if (HIDE_SEEKBAR_THUMBNAIL_ENABLED) {
                return 0x0;
            }

            return customSeekbarColor;
        }

        return colorValue;
    }

    private static String colorArrayToHex(int[] colors) {
        final int length = colors.length;
        StringBuilder builder = new StringBuilder(length * 12);
        builder.append("[");

        int i = 0;
        for (int color : colors) {
            builder.append(String.format("#%X", color));
            if (++i < length) {
                builder.append(", ");
            }
        }

        builder.append("]");
        return builder.toString();
    }

    /**
     * Injection point.
     * 19.49+
     */
    public static int[] getPlayerLinearGradient(int[] original, int x0, int y1) {
        // This hook is used for both the player and the feed.
        // Feed usage always has x0 and y1 value of zero, and the player is always non zero.
        if (HIDE_SEEKBAR_THUMBNAIL_ENABLED && x0 == 0 && y1 == 0) {
            return HIDDEN_SEEKBAR_GRADIENT_COLORS;
        }
        return getPlayerLinearGradient(original);
    }

    /**
     * Injection point.
     * Pre 19.49
     */
    public static int[] getPlayerLinearGradient(int[] original) {
        return CUSTOM_SEEKBAR_COLOR_ENABLED
                ? customSeekbarColorGradient
                : original;
    }

    /**
     * Injection point.
     */
    public static int[] getLithoLinearGradient(int[] colors, float[] positions) {
        if (CUSTOM_SEEKBAR_COLOR_ENABLED || HIDE_SEEKBAR_THUMBNAIL_ENABLED) {
            // Most litho usage of linear gradients is hooked here,
            // so must only change if the values are those for the seekbar.
            if ((Arrays.equals(FEED_ORIGINAL_SEEKBAR_GRADIENT_COLORS, colors)
                    && Arrays.equals(FEED_ORIGINAL_SEEKBAR_GRADIENT_POSITIONS, positions))) {
                return HIDE_SEEKBAR_THUMBNAIL_ENABLED
                        ? HIDDEN_SEEKBAR_GRADIENT_COLORS
                        : customSeekbarColorGradient;
            }

            Logger.printDebug(() -> "Ignoring gradient colors: " + colorArrayToHex(colors)
                    + " positions: " + Arrays.toString(positions));
        }

        return colors;
    }

    /**
     * Injection point
     * <p>
     * Set seekbar thumb color
     * The seekbar thumb was initially set to the gradient seekbar's starting color.
     * But we will switch to using the end color.
     */
    public static int setSeekbarThumbColor() {
        try {
            return Color.parseColor(Settings.CUSTOM_SEEKBAR_COLOR_ACCENT.get());
        } catch (Exception ex) {
            Utils.showToastShort(str("revanced_color_invalid_toast"));
            Utils.showToastShort(str("revanced_extended_reset_to_default_toast"));
            Settings.CUSTOM_SEEKBAR_COLOR_ACCENT.resetToDefault();
            return setSeekbarThumbColor();
        }
    }

    /**
     * Injection point
     * <p>
     * Overrides default positions for gradient seekbar
     */
    public static void setSeekbarGradientPositions(float[] positions) {
        try {
            String[] positionStrings = Settings.GRADIENT_SEEKBAR_POSITIONS.get().split(",");

            // Check if input length matches the expected length
            if (positionStrings.length != positions.length) {
                Utils.showToastShort(str("revanced_gradient_seekbar_positions_reset"));
                Settings.GRADIENT_SEEKBAR_POSITIONS.resetToDefault();
                return;
            }

            float[] newPositions = new float[positions.length];

            for (int i = 0; i < positions.length; i++) {
                float position = Float.parseFloat(positionStrings[i].trim());

                // Ensure positions are in valid range [0.0, 1.0]
                if (position < 0.0f || position > 1.0f) {
                    Utils.showToastShort(str("revanced_gradient_seekbar_positions_reset"));
                    Settings.GRADIENT_SEEKBAR_POSITIONS.resetToDefault();
                    return;
                }
                newPositions[i] = position;
            }

            // Update positions array if all values are valid
            System.arraycopy(newPositions, 0, positions, 0, positions.length);
        } catch (Exception ex) {
            Utils.showToastShort(str("revanced_gradient_seekbar_positions_reset"));
            Settings.GRADIENT_SEEKBAR_POSITIONS.resetToDefault();
            setSeekbarGradientPositions(positions);
        }
    }

    /**
     * Injection point.
     * <p>
     * Overrides color when video player seekbar is clicked.
     */
    public static int getVideoPlayerSeekbarClickedColor(int colorValue) {
        if (!CUSTOM_SEEKBAR_COLOR_ENABLED) {
            return colorValue;
        }

        return colorValue == ORIGINAL_SEEKBAR_COLOR
                ? getSeekbarColorValue(ORIGINAL_SEEKBAR_COLOR)
                : colorValue;
    }

    /**
     * Injection point.
     * <p>
     * Overrides color used for the video player seekbar.
     */
    public static int getVideoPlayerSeekbarColor(int originalColor) {
        return CUSTOM_SEEKBAR_COLOR_ENABLED
                ? getSeekbarColorValue(originalColor)
                : originalColor;
    }

    /**
     * Injection point.
     * <p>
     * Overrides color used for the video player seekbar.
     */
    public static int getVideoPlayerSeekbarColorAccent(int originalColor) {
        return CUSTOM_SEEKBAR_COLOR_ENABLED
                ? customSeekbarColorAccent
                : originalColor;
    }

    /**
     * Color parameter is changed to the custom seekbar color, while retaining
     * the brightness and alpha changes of the parameter value compared to the original seekbar color.
     */
    private static int getSeekbarColorValue(int originalColor) {
        try {
            final int alphaDifference = Color.alpha(originalColor) - Color.alpha(ORIGINAL_SEEKBAR_COLOR);

            // The seekbar uses the same color but different brightness for different situations.
            float[] hsv = new float[3];
            Color.colorToHSV(originalColor, hsv);
            final float brightnessDifference = hsv[2] - ORIGINAL_SEEKBAR_COLOR_BRIGHTNESS;

            // Apply the brightness difference to the custom seekbar color.
            hsv[0] = customSeekbarColorHSV[0];
            hsv[1] = customSeekbarColorHSV[1];
            hsv[2] = clamp(customSeekbarColorHSV[2] + brightnessDifference, 0, 1);

            final int replacementAlpha = clamp(Color.alpha(customSeekbarColor) + alphaDifference, 0, 255);
            final int replacementColor = Color.HSVToColor(replacementAlpha, hsv);
            Logger.printDebug(() -> String.format("Original color: #%08X  replacement color: #%08X",
                    originalColor, replacementColor));
            return replacementColor;
        } catch (Exception ex) {
            Logger.printException(() -> "getSeekbarColorValue failure", ex);
            return originalColor;
        }
    }
}
