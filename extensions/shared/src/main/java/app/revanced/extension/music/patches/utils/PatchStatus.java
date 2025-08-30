package app.revanced.extension.music.patches.utils;

import static app.revanced.extension.music.utils.ExtendedUtils.IS_6_36_OR_GREATER;
import static app.revanced.extension.music.utils.ExtendedUtils.IS_7_17_OR_GREATER;
import static app.revanced.extension.music.utils.ExtendedUtils.IS_8_28_OR_GREATER;

import app.revanced.extension.music.patches.spoof.ClientType;

@SuppressWarnings("unused")
public class PatchStatus {
    public static boolean DarkTheme() {
        // Replace this with true if the 'Dark theme' patch succeeds
        return false;
    }

    public static ClientType DefaultClientType() {
        if (IS_8_28_OR_GREATER) {
            return ClientType.IOS_MUSIC_8_34_BLOCK_REQUEST;
        } else if (IS_7_17_OR_GREATER) {
            return ClientType.IOS_MUSIC_8_12_BLOCK_REQUEST;
        } else if (IS_6_36_OR_GREATER) {
            return ClientType.IOS_MUSIC_7_04_BLOCK_REQUEST;
        } else {
            return ClientType.IOS_MUSIC_6_21_BLOCK_REQUEST;
        }
    }

    public static boolean SpoofClient() {
        // Replace this with true If the 'Spoof client' succeeds.
        return false;
    }

    public static boolean SpoofVideoStreams() {
        // Replace this with true If the 'Spoof video streams' succeeds.
        return false;
    }

    public static boolean SpoofAppVersionDefaultBoolean() {
        return false;
    }

    public static String SpoofAppVersionDefaultString() {
        return "6.42.55";
    }
}
