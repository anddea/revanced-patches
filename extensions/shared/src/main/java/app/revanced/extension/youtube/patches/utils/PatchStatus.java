package app.revanced.extension.youtube.patches.utils;

public class PatchStatus {

    public static boolean ImageSearchButton() {
        // Replace this with true if the Hide image search buttons patch succeeds
        return false;
    }

    public static boolean MinimalHeader() {
        // Replace this with true If the Custom header patch succeeds and the patch option was `youtube_minimal_header`
        return false;
    }

    public static boolean PlayerButtons() {
        // Replace this with true if the Hide player buttons patch succeeds
        return false;
    }

    public static boolean QuickActions() {
        // Replace this with true if the Fullscreen components patch succeeds
        return false;
    }

    public static boolean RememberPlaybackSpeed() {
        // Replace this with true if the Video playback patch succeeds
        return false;
    }

    public static boolean SponsorBlock() {
        // Replace this with true if the SponsorBlock patch succeeds
        return false;
    }

    public static boolean ToolBarComponents() {
        // Replace this with true if the Toolbar components patch succeeds
        return false;
    }

    // Modified by a patch. Do not touch.
    public static String RVXMusicPackageName() {
        return "com.google.android.apps.youtube.music";
    }

    // Modified by a patch. Do not touch.
    public static boolean OldSeekbarThumbnailsDefaultBoolean() {
        return false;
    }

}
