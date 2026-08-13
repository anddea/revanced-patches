package app.morphe.extension.youtube.patches.utils;

public class PatchStatus {
    public static final String SPOOF_APP_VERSION_TARGET_DEFAULT_VALUE = "20.05.46";

    public static boolean ImageSearchButton() {
        // Replace this with true if the 'Hide image search buttons' patch succeeds
        return false;
    }


    public static boolean OldSplashAnimation() {
        // Replace this with true if the 'Restore old splash animation (Custom branding icon)' succeeds
        return false;
    }

    public static boolean PlayerButtons() {
        // Replace this with true if the 'Hide player buttons' patch succeeds
        return false;
    }

    public static boolean QuickActions() {
        // Replace this with true if the 'Fullscreen components' patch succeeds
        return false;
    }

    public static boolean SponsorBlock() {
        // Replace this with true if the 'SponsorBlock' patch succeeds
        return false;
    }

    public static boolean ToolBarComponents() {
        // Replace this with true if the 'Toolbar components' patch succeeds
        return false;
    }

    public static boolean VideoPlayback() {
        // Replace this with true if the 'Video playback' patch succeeds
        return false;
    }

    public static boolean VoiceOverTranslation() {
        // Replace this with true if the 'Voice Over Translation' patch succeeds
        return false;
    }

    public static boolean SpoofAppVersion() {
        // Replace this with true if the 'Spoof app version' patch succeeds
        return false;
    }

    // Modified by a patch. Do not touch.
    public static boolean SpoofAppVersionDefaultBoolean() {
        return false;
    }

    public static String SpoofAppVersionDefaultString() {
        return SPOOF_APP_VERSION_TARGET_DEFAULT_VALUE;
    }

    // Modified by a patch. Do not touch.
    public static String TargetActivityClass() {
        return "";
    }

}
