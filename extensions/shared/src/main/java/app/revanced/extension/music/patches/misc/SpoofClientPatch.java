package app.revanced.extension.music.patches.misc;

import app.revanced.extension.music.patches.misc.client.AppClient.ClientType;
import app.revanced.extension.music.settings.Settings;

@SuppressWarnings("unused")
public class SpoofClientPatch {
    private static final boolean SPOOF_CLIENT_ENABLED = Settings.SPOOF_CLIENT.get();
    private static final ClientType clientType = ClientType.IOS_MUSIC;

    /**
     * Injection point.
     */
    public static int getClientTypeId(int originalClientTypeId) {
        if (SPOOF_CLIENT_ENABLED) {
            return clientType.id;
        }

        return originalClientTypeId;
    }

    /**
     * Injection point.
     */
    public static String getClientVersion(String originalClientVersion) {
        if (SPOOF_CLIENT_ENABLED) {
            return clientType.clientVersion;
        }

        return originalClientVersion;
    }

    /**
     * Injection point.
     */
    public static String getClientModel(String originalClientModel) {
        if (SPOOF_CLIENT_ENABLED) {
            return clientType.deviceModel;
        }

        return originalClientModel;
    }

    /**
     * Injection point.
     */
    public static String getOsVersion(String originalOsVersion) {
        if (SPOOF_CLIENT_ENABLED) {
            return clientType.osVersion;
        }

        return originalOsVersion;
    }

    /**
     * Injection point.
     */
    public static String getUserAgent(String originalUserAgent) {
        if (SPOOF_CLIENT_ENABLED) {
            return clientType.userAgent;
        }

        return originalUserAgent;
    }

    /**
     * Injection point.
     */
    public static boolean isClientSpoofingEnabled() {
        return SPOOF_CLIENT_ENABLED;
    }

    /**
     * Injection point.
     * When spoofing the client to iOS, the playback speed menu is missing from the player response.
     * Return true to force create the playback speed menu.
     */
    public static boolean forceCreatePlaybackSpeedMenu(boolean original) {
        if (SPOOF_CLIENT_ENABLED) {
            return true;
        }
        return original;
    }
}