package app.revanced.extension.music.patches.utils;

import static app.revanced.extension.music.utils.ExtendedUtils.IS_6_36_OR_GREATER;
import static app.revanced.extension.music.utils.ExtendedUtils.IS_7_17_OR_GREATER;

import app.revanced.extension.music.patches.spoof.ClientType;

@SuppressWarnings("unused")
public class PatchStatus {
    public static boolean DarkTheme() {
        // Replace this with true if the Dark theme patch succeeds
        return false;
    }

    public static ClientType DefaultClientType() {
        if (IS_7_17_OR_GREATER) {
            return ClientType.IOS_MUSIC_8_12;
        } else if (IS_6_36_OR_GREATER) {
            return ClientType.IOS_MUSIC_7_04;
        } else {
            return ClientType.IOS_MUSIC_6_21;
        }
    }

    public static boolean SpoofAppVersionDefaultBoolean() {
        return false;
    }

    public static String SpoofAppVersionDefaultString() {
        return "6.42.55";
    }
}
