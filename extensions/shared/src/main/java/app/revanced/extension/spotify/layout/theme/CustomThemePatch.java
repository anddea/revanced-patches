package app.revanced.extension.spotify.layout.theme;

import android.graphics.Color;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.Utils;

@SuppressWarnings("unused")
public final class CustomThemePatch {

    private static final int BACKGROUND_COLOR = getColorFromString("@color/gray_7");
    private static final int BACKGROUND_COLOR_SECONDARY = getColorFromString("@color/gray_15");
    private static final int ACCENT_COLOR = getColorFromString("@color/spotify_green_157");
    private static final int ACCENT_PRESSED_COLOR =
            getColorFromString("@color/dark_brightaccent_background_press");

    /**
     * Returns an int representation of the color resource or hex code.
     */
    private static int getColorFromString(String colorString) {
        try {
            return Utils.getColorFromString(colorString);
        } catch (Exception ex) {
            Logger.printException(() -> "Invalid color string: " + colorString, ex);
            return Color.BLACK;
        }
    }

    /**
     * Injection point. Returns an int representation of the replaced color from the original color.
     */
    public static int replaceColor(int originalColor) {
        return switch (originalColor) {
            // Playlist background color.
            case 0xFF121212 -> BACKGROUND_COLOR;

            // Share menu background color.
            // Home category pills background color.
            // Settings header background color.
            // Spotify Connect device list background color.
            case 0xFF1F1F1F, 0xFF333333, 0xFF282828, 0xFF2A2A2A -> BACKGROUND_COLOR_SECONDARY;

            // Some Lottie animations have a color that's slightly off due to rounding errors.
            // Intermediate color used in some animations, same rounding issue.
            case 0xFF1ED760, 0xFF1ED75F, 0xFF1DB954, 0xFF1CB854 -> ACCENT_COLOR;
            case 0xFF1ABC54 -> ACCENT_PRESSED_COLOR;
            default -> originalColor;
        };
    }
}
