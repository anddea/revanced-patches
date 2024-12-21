package app.revanced.extension.shared.patches;

import app.revanced.extension.shared.patches.client.AppClient.ClientType;

@SuppressWarnings("unused")
public class PatchStatus {
    public static boolean HideFullscreenAdsDefaultBoolean() {
        return false;
    }

    public static ClientType SpoofStreamingDataDefaultClient() {
        return ClientType.IOS;
    }

    public static boolean SpoofClient() {
        // Replace this with true If the Spoof client patch succeeds
        return false;
    }

    public static boolean SpoofStreamingData() {
        // Replace this with true If the Spoof streaming data patch succeeds
        return false;
    }
}
