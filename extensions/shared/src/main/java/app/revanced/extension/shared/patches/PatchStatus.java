package app.revanced.extension.shared.patches;

@SuppressWarnings("unused")
public class PatchStatus {
    public static boolean SpoofClient() {
        // Replace this with true If the Spoof client patch succeeds in YouTube Music.
        return false;
    }

    public static boolean SpoofStreamingData() {
        // Replace this with true If the Spoof streaming data patch succeeds in YouTube.
        return false;
    }

    public static boolean SpoofStreamingDataIOS() {
        return false;
    }
}
