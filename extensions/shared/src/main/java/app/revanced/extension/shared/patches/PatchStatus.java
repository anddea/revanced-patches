package app.revanced.extension.shared.patches;

@SuppressWarnings("unused")
public class PatchStatus {
    public static boolean HideFullscreenAdsDefaultBoolean() {
        return false;
    }

    public static boolean SpoofStreamingData() {
        // Replace this with true If the Spoof streaming data patch succeeds
        return false;
    }

    public static boolean SpoofStreamingDataAndroidOnlyDefaultBoolean() {
        // Replace this with true If the Spoof streaming data patch succeeds in YouTube
        return false;
    }
}
