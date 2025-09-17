package app.revanced.extension.shared.patches;

import app.revanced.extension.shared.innertube.client.YouTubeClient.ClientType;

@SuppressWarnings("unused")
public class PatchStatus {
    public static boolean SpoofStreamingData() {
        // Replace this with true If the 'Spoof streaming data' patch succeeds.
        return false;
    }

    public static boolean SpoofStreamingDataYouTube() {
        // Replace this with true If the 'Spoof streaming data' patch succeeds in YouTube.
        return false;
    }

    public static ClientType SpoofStreamingDataDefaultClient() {
        return SpoofStreamingDataYouTube()
                ? ClientType.ANDROID_VR
                : ClientType.TV_SIMPLY;
    }

    public static String PatchVersion() {
        return "Unknown";
    }

    public static long PatchedTime() {
        return 0L;
    }
}
