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
}
