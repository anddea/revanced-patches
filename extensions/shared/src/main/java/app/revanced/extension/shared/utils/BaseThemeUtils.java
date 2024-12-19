package app.revanced.extension.shared.utils;

import static app.revanced.extension.shared.utils.ResourceUtils.getColor;
import static app.revanced.extension.shared.utils.ResourceUtils.getColorIdentifier;

import android.graphics.Color;

@SuppressWarnings("unused")
public class BaseThemeUtils {
    private static int themeValue = 1;

    /**
     * Injection point.
     */
    public static void setTheme(Enum<?> value) {
        final int newOrdinalValue = value.ordinal();
        if (themeValue != newOrdinalValue) {
            themeValue = newOrdinalValue;
            Logger.printDebug(() -> "Theme value: " + newOrdinalValue);
        }
    }

    public static boolean isDarkTheme() {
        return themeValue == 1;
    }

    public static String getColorHexString(int color) {
        return String.format("#%06X", (0xFFFFFF & color));
    }

    /**
     * Subclasses can override this and provide a themed color.
     */
    public static int getLightColor() {
        return Color.WHITE;
    }

    /**
     * Subclasses can override this and provide a themed color.
     */
    public static int getDarkColor() {
        return Color.BLACK;
    }

    public static String getBackgroundColorHexString() {
        return getColorHexString(getBackgroundColor());
    }

    public static String getForegroundColorHexString() {
        return getColorHexString(getForegroundColor());
    }

    public static int getBackgroundColor() {
        final String colorName = isDarkTheme() ? "yt_black1" : "yt_white1";
        final int colorIdentifier = getColorIdentifier(colorName);
        if (colorIdentifier != 0) {
            return getColor(colorName);
        } else {
            return isDarkTheme() ? getDarkColor() : getLightColor();
        }
    }

    public static int getForegroundColor() {
        final String colorName = isDarkTheme() ? "yt_white1" : "yt_black1";
        final int colorIdentifier = getColorIdentifier(colorName);
        if (colorIdentifier != 0) {
            return getColor(colorName);
        } else {
            return isDarkTheme() ? getLightColor() : getDarkColor();
        }
    }

}
