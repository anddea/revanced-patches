package app.morphe.extension.shared.patches;

@SuppressWarnings("unused")
public class PatchStatus {

    // Modified by a patch. Do not touch.
    public static String PackageNameYouTube() {
        return "com.google.android.youtube";
    }

    // Modified by a patch. Do not touch.
    public static String PackageNameYouTubeMusic() {
        return "com.google.android.apps.youtube.music";
    }

    public static boolean SpoofStreamingData() {
        // Replace this with true If the 'Spoof streaming data' patch succeeds.
        return false;
    }

    // Modified by a patch. Do not touch.
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean GmsCoreSupport() {
        // Replace this with true if the 'GmsCore support' patch succeeds.
        return false;
    }

    // Modified by a patch. Do not touch.
    public static boolean PoTokenProvider() {
        // Replace this with true if the 'PoToken provider' patch succeeds.
        return false;
    }

    public static String PatchVersion() {
        return "Unknown";
    }

    public static long PatchedTime() {
        return 0L;
    }

    // Modified by a patch. Do not touch.
    public static String WebViewActivityClass() {
        return "";
    }
}
