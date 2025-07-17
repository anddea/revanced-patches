package app.revanced.extension.youtube.utils;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import androidx.annotation.Nullable;
import app.revanced.extension.shared.utils.BaseThemeUtils;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.ResourceUtils;
import app.revanced.extension.shared.utils.Utils;
import app.revanced.extension.youtube.settings.Settings;

import java.util.regex.Pattern;

import static app.revanced.extension.shared.utils.Utils.getResources;

@SuppressWarnings({"unused", "SameParameterValue"})
public class ThemeUtils extends BaseThemeUtils {
    // Static pattern for validating hex colors (#FFFFFF or #000000)
    private static final Pattern INVALID_HEX_PATTERN = Pattern.compile("^#?(FFFFFF|000000)$", Pattern.CASE_INSENSITIVE);

    // Cached background colors for light and dark themes
    @Nullable
    private static Integer lightThemeColor;
    @Nullable
    private static Integer darkThemeColor;
    // Cached highlight color
    @Nullable
    private static Integer cachedHighlightColor;
    private static boolean lastThemeWasDark; // Tracks the last theme to detect changes

    public static int getThemeId() {
        final String themeName = isDarkModeEnabled()
                ? "Theme.YouTube.Settings.Dark"
                : "Theme.YouTube.Settings";
        return ResourceUtils.getStyleIdentifier(themeName);
    }

    public static Drawable getBackButtonDrawable() {
        final String drawableName = isDarkModeEnabled()
                ? "yt_outline_arrow_left_white_24"
                : "yt_outline_arrow_left_black_24";
        return ResourceUtils.getDrawable(drawableName);
    }

    public static Drawable getTrashButtonDrawable() {
        final String drawableName = isDarkModeEnabled()
                ? "yt_outline_trash_can_white_24"
                : "yt_outline_trash_can_black_24";
        return ResourceUtils.getDrawable(drawableName);
    }

    public static int getDialogBackgroundColor() {
        final String colorName = isDarkModeEnabled()
                ? "yt_black1"
                : "yt_white1";

        return Utils.getColorFromString(colorName);
    }

    /**
     * Adjusts the background color based on the current theme.
     *
     * @param isHandleBar If true, applies a stronger darkening factor (0.9) for the handle bar in light theme;
     *                    if false, applies a standard darkening factor (0.95) for other elements in light theme.
     * @return A modified background color, lightened by 20% for dark themes or darkened by 5% (or 10% for handle bar)
     * for light themes to ensure visual contrast.
     */
    public static int getAdjustedBackgroundColor(boolean isHandleBar) {
        final int baseColor = getDialogBackgroundColor();
        float darkThemeFactor = isHandleBar ? 1.25f : 1.115f; // 1.25f for handleBar, 1.115f for others in dark theme.
        float lightThemeFactor = isHandleBar ? 0.9f : 0.95f; // 0.9f for handleBar, 0.95f for others in light theme.
        return isDarkModeEnabled()
                ? adjustColorBrightness(baseColor, darkThemeFactor)  // Lighten for dark theme.
                : adjustColorBrightness(baseColor, lightThemeFactor); // Darken for light theme.
    }

    /**
     * Since {@link android.widget.Toolbar} is used instead of {@link android.support.v7.widget.Toolbar},
     * We have to manually specify the toolbar background.
     *
     * @return toolbar background color.
     */
    public static int getToolbarBackgroundColor() {
        final String colorName = isDarkModeEnabled()
                ? "yt_black3"
                : "yt_white1";
        return ResourceUtils.getColor(colorName);
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
            lightThemeColor = null;
            darkThemeColor = null;
            cachedHighlightColor = null;
            lastThemeWasDark = isDark;
        }

        if (isDark) {
            if (darkThemeColor == null) {
                darkThemeColor = ResourceUtils.getColor("yt_black3");
                Logger.printDebug(() -> "Cached dark theme color: " + darkThemeColor);
            }
            return darkThemeColor;
        } else {
            if (lightThemeColor == null) {
                lightThemeColor = ResourceUtils.getColor("yt_white1");
                Logger.printDebug(() -> "Cached light theme color: " + lightThemeColor);
            }
            return lightThemeColor;
        }
    }

    /**
     * Clears the cached theme colors, forcing a refresh on the next call to getBackgroundColor()
     * and getHighlightColor(). Call this when the theme changes or settings are updated.
     */
    public static void clearThemeCache() {
        lightThemeColor = null;
        darkThemeColor = null;
        cachedHighlightColor = null;
        Logger.printDebug(() -> "Cleared theme color cache");
    }

    public static int getPressedElementColor() {
        int baseColor = getBackgroundColor();
        float factor = isDarkModeEnabled() ? 1.15f : 0.85f;
        return adjustColorBrightness(baseColor, factor);
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

    /**
     * Adjusts the brightness of a color by lightening or darkening it.
     *
     * @param color  The input color in ARGB format.
     * @param factor The adjustment factor (>1.0f to lighten, <=1.0f to darken).
     * @return The adjusted color in ARGB format.
     */
    public static int adjustColorBrightness(int color, float factor) {
        int alpha = Color.alpha(color);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);

        if (factor > 1.0f) {
            // Lighten: Interpolate toward white
            float t = 1.0f - (1.0f / factor);
            red = Math.round(red + (255 - red) * t);
            green = Math.round(green + (255 - green) * t);
            blue = Math.round(blue + (255 - blue) * t);
        } else {
            // Darken: Scale toward black
            red = Math.round(red * factor);
            green = Math.round(green * factor);
            blue = Math.round(blue * factor);
        }

        // Clamp values to [0, 255]
        red = Math.max(0, Math.min(255, red));
        green = Math.max(0, Math.min(255, green));
        blue = Math.max(0, Math.min(255, blue));

        return Color.argb(alpha, red, green, blue);
    }
}
