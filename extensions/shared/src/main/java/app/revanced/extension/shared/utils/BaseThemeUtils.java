package app.revanced.extension.shared.utils;

import static app.revanced.extension.shared.utils.Utils.clamp;
import static app.revanced.extension.shared.utils.Utils.getResources;

import android.content.res.Configuration;
import android.graphics.Color;

import android.graphics.drawable.GradientDrawable;
import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import app.revanced.extension.youtube.settings.Settings;

import java.util.regex.Pattern;

@SuppressWarnings({"unused", "SameReturnValue"})
public class BaseThemeUtils {
    // Static pattern for validating hex colors (#FFFFFF or #000000)
    private static final Pattern INVALID_HEX_PATTERN = Pattern.compile("^#?(FFFFFF|000000)$", Pattern.CASE_INSENSITIVE);

    // Must initially be a non-valid enum ordinal value.
    private static int currentThemeValueOrdinal = -1;

    @ColorInt
    private static int darkColor = Color.BLACK;
    @ColorInt
    private static int lightColor = Color.WHITE;

    @Nullable
    private static Boolean isDarkModeEnabled;

    @Nullable
    private static Integer cachedHighlightColor;
    private static boolean lastThemeWasDark;


    // For YouTube Music, Modern dialog not yet supported.
    public static boolean isSupportModernDialog = true;

    /**
     * Injection point.
     * <p>
     * Forces dark mode since YT Music does not support light theme.
     */
    public static void updateDarkModeStatus() {
        isDarkModeEnabled = Boolean.TRUE;
        isSupportModernDialog = false;
        Logger.printDebug(() -> "Dark mode status: " + isDarkModeEnabled);
    }

    /**
     * Injection point.
     * <p>
     * Updates dark/light mode since YT settings can force light/dark mode
     * which can differ from the global device settings.
     */
    public static void updateLightDarkModeStatus(Enum<?> value) {
        final int newOrdinalValue = value.ordinal();
        if (currentThemeValueOrdinal != newOrdinalValue) {
            currentThemeValueOrdinal = newOrdinalValue;
            isDarkModeEnabled = newOrdinalValue == 1;
            Logger.printDebug(() -> "Dark mode status: " + isDarkModeEnabled);
        }
    }

    /**
     * @return The current dark mode as set by any patch.
     * Or if none is set, then the system dark mode status is returned.
     */
    public static boolean isDarkModeEnabled() {
        Boolean isDarkMode = isDarkModeEnabled;
        if (isDarkMode != null) {
            return isDarkMode;
        }

        Configuration config = Utils.getResources(false).getConfiguration();
        final int currentNightMode = config.uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    /**
     * Gets the background color for the current theme, using cached values if available.
     * Caches the color for both light and dark themes to avoid repeated resource lookups.
     *
     * @return The background color for the current theme.
     */
    public static int getBackgroundColor() {
        boolean isDark = isDarkModeEnabled();
        // Check if theme has changed to invalidate cache if needed
        if (lastThemeWasDark != isDark) {
            cachedHighlightColor = null;
            lastThemeWasDark = isDark;
        }

        if (isDark) {
            return darkColor;
        } else {
            return lightColor;
        }
    }

    public static void setThemeColor() {
        setThemeLightColor(getThemeColor(getThemeLightColorResourceName(), Color.WHITE));
        setThemeDarkColor(getThemeColor(getThemeDarkColorResourceName(), Color.BLACK));
    }

    /**
     * Sets the theme light color used by the app.
     */
    public static void setThemeLightColor(@ColorInt int color) {
        Logger.printDebug(() -> "Setting theme light color: " + getColorHexString(color));
        lightColor = color;
    }

    /**
     * Sets the theme dark used by the app.
     */
    public static void setThemeDarkColor(@ColorInt int color) {
        Logger.printDebug(() -> "Setting theme dark color: " + getColorHexString(color));
        darkColor = color;
    }

    /**
     * Returns the themed light color, or {@link Color#WHITE} if no theme was set using
     * {@link #setThemeLightColor(int).
     */
    @ColorInt
    public static int getThemeLightColor() {
        return lightColor;
    }

    /**
     * Returns the themed dark color, or {@link Color#BLACK} if no theme was set using
     * {@link #setThemeDarkColor(int)}.
     */
    @ColorInt
    public static int getThemeDarkColor() {
        return darkColor;
    }

    /**
     * Injection point.
     */
    private static String getThemeLightColorResourceName() {
        // Value is changed by Settings patch.
        return "#FFFFFFFF";
    }

    /**
     * Injection point.
     */
    private static String getThemeDarkColorResourceName() {
        // Value is changed by Settings patch.
        return "#FF000000";
    }

    @ColorInt
    private static int getThemeColor(String resourceName, int defaultColor) {
        try {
            return ResourceUtils.getColor(resourceName);
        } catch (Exception ex) {
            // This code can never be reached since a bad custom color will
            // fail during resource compilation. So no localized strings are needed here.
            Logger.printException(() -> "Invalid custom theme color: " + resourceName, ex);
            return defaultColor;
        }
    }

    @ColorInt
    public static int getDialogBackgroundColor() {
        if (isDarkModeEnabled()) {
            final int darkColor = getThemeDarkColor();
            return darkColor == Color.BLACK
                    // Lighten the background a little if using AMOLED dark theme
                    // as the dialogs are almost invisible.
                    ? 0xFF080808 // 3%
                    : darkColor;
        }
        return getThemeLightColor();
    }

    /**
     * @return The current app background color.
     */
    @ColorInt
    public static int getAppBackgroundColor() {
        return isDarkModeEnabled() ? getThemeDarkColor() : getThemeLightColor();
    }

    /**
     * @return The current app foreground color.
     */
    @ColorInt
    public static int getAppForegroundColor() {
        return isDarkModeEnabled()
                ? getThemeLightColor()
                : getThemeDarkColor();
    }

    @ColorInt
    public static int getOkButtonBackgroundColor() {
        return isDarkModeEnabled()
                // Must be inverted color.
                ? Color.WHITE
                : Color.BLACK;
    }

    @ColorInt
    public static int getCancelOrNeutralButtonBackgroundColor() {
        return isDarkModeEnabled()
                ? adjustColorBrightness(getDialogBackgroundColor(), 1.10f)
                : adjustColorBrightness(getThemeLightColor(), 0.95f);
    }

    @ColorInt
    public static int getEditTextBackground() {
        return isDarkModeEnabled()
                ? adjustColorBrightness(getDialogBackgroundColor(), 1.05f)
                : adjustColorBrightness(getThemeLightColor(), 0.97f);
    }

    public static String getColorHexString(@ColorInt int color) {
        return String.format("#%06X", (0x00FFFFFF & color));
    }

    public static String getBackgroundColorHexString() {
        return getColorHexString(getAppBackgroundColor());
    }

    public static String getForegroundColorHexString() {
        return getColorHexString(getAppForegroundColor());
    }

    /**
     * Uses {@link #adjustColorBrightness(int, float)} depending if light or dark mode is active.
     */
    @ColorInt
    public static int adjustColorBrightness(@ColorInt int baseColor, float lightThemeFactor, float darkThemeFactor) {
        return isDarkModeEnabled()
                ? adjustColorBrightness(baseColor, darkThemeFactor)
                : adjustColorBrightness(baseColor, lightThemeFactor);
    }

    /**
     * Adjusts the brightness of a color by lightening or darkening it based on the given factor.
     * <p>
     * If the factor is greater than 1, the color is lightened by interpolating toward white (#FFFFFF).
     * If the factor is less than or equal to 1, the color is darkened by scaling its RGB components toward black (#000000).
     * The alpha channel remains unchanged.
     *
     * @param color  The input color to adjust, in ARGB format.
     * @param factor The adjustment factor. Use values > 1.0f to lighten (e.g., 1.11f for slight lightening)
     *               or values <= 1.0f to darken (e.g., 0.95f for slight darkening).
     * @return The adjusted color in ARGB format.
     */
    @ColorInt
    public static int adjustColorBrightness(@ColorInt int color, float factor) {
        final int alpha = Color.alpha(color);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);

        if (factor > 1.0f) {
            // Lighten: Interpolate toward white (255).
            final float t = 1.0f - (1.0f / factor); // Interpolation parameter.
            red = Math.round(red + (255 - red) * t);
            green = Math.round(green + (255 - green) * t);
            blue = Math.round(blue + (255 - blue) * t);
        } else {
            // Darken or no change: Scale toward black.
            red = (int) (red * factor);
            green = (int) (green * factor);
            blue = (int) (blue * factor);
        }

        // Ensure values are within [0, 255].
        red = clamp(red, 0, 255);
        green = clamp(green, 0, 255);
        blue = clamp(blue, 0, 255);

        return Color.argb(alpha, red, green, blue);
    }

    public static GradientDrawable getSearchViewShape() {
        GradientDrawable shape = new GradientDrawable();
        int baseColor = getBackgroundColor();
        int adjustedColor = isDarkModeEnabled()
                ? adjustColorBrightness(baseColor, 1.15f)
                : adjustColorBrightness(baseColor, 0.85f);
        shape.setColor(adjustedColor);
        shape.setCornerRadius(30 * getResources().getDisplayMetrics().density);
        return shape;
    }

    /**
     * Gets the highlight color for search, caching it to avoid repeated calculations.
     * Uses SETTINGS_SEARCH_HIGHLIGHT_COLOR, falling back to the background color if the setting
     * is invalid or set to #FFFFFF/#000000. Adjusts brightness based on the current theme.
     *
     * @return The highlight color in ARGB format.
     */
    public static int getHighlightColor() {
        if (cachedHighlightColor == null) {
            String hexColor = Settings.SETTINGS_SEARCH_HIGHLIGHT_COLOR.get();
            int baseColor;
            if (INVALID_HEX_PATTERN.matcher(hexColor).matches()) {
                baseColor = getBackgroundColor();
            } else {
                try {
                    baseColor = Color.parseColor(hexColor);
                } catch (IllegalArgumentException e) {
                    baseColor = getBackgroundColor();
                    Logger.printDebug(() -> "Invalid highlight color: " + hexColor + ", using background color");
                }
            }
            float factor = isDarkModeEnabled() ? 1.30f : 0.90f; // Match new code's factors
            cachedHighlightColor = adjustColorBrightness(baseColor, factor);
            Logger.printDebug(() -> "Cached highlight color: " + cachedHighlightColor);
        }
        return cachedHighlightColor;
    }
}
