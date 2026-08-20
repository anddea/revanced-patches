package app.morphe.extension.music.patches.utils;

import static app.morphe.extension.music.utils.ExtendedUtils.IS_7_25_OR_GREATER;
import static app.morphe.extension.music.utils.ExtendedUtils.IS_8_00_OR_GREATER;

@SuppressWarnings("unused")
public class PatchStatus {
    public static boolean DarkTheme() {
        // Replace this with true if the 'Dark theme' patch succeeds
        return false;
    }

    public static String SpoofAppVersionDefaultString() {
        if (!IS_8_00_OR_GREATER) {
            return "8.05.50";
        }
        return IS_7_25_OR_GREATER ? "6.42.55" : "6.35.52";
    }

    public static boolean SpoofAppVersionDefaultBoolean() {
        return !IS_8_00_OR_GREATER;
    }
}
