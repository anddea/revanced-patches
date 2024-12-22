package app.revanced.extension.music.patches.misc;

import app.revanced.extension.music.patches.misc.client.AppClient.ClientType;
import app.revanced.extension.music.settings.Settings;

@SuppressWarnings("unused")
public class SpoofClientPatch {
    private static final ClientType CLIENT_TYPE = Settings.SPOOF_CLIENT_TYPE.get();
    public static final boolean SPOOF_CLIENT = Settings.SPOOF_CLIENT.get();

    /**
     * Injection point.
     */
    public static int getClientTypeId(int originalClientTypeId) {
        if (SPOOF_CLIENT) {
            return CLIENT_TYPE.id;
        }

        return originalClientTypeId;
    }

    /**
     * Injection point.
     */
    public static String getClientVersion(String originalClientVersion) {
        if (SPOOF_CLIENT) {
            return CLIENT_TYPE.clientVersion;
        }

        return originalClientVersion;
    }

    /**
     * Injection point.
     */
    public static String getClientModel(String originalClientModel) {
        if (SPOOF_CLIENT) {
            return CLIENT_TYPE.deviceModel;
        }

        return originalClientModel;
    }

    /**
     * Injection point.
     */
    public static String getOsVersion(String originalOsVersion) {
        if (SPOOF_CLIENT) {
            return CLIENT_TYPE.osVersion;
        }

        return originalOsVersion;
    }

    /**
     * Injection point.
     */
    public static String getUserAgent(String originalUserAgent) {
        if (SPOOF_CLIENT) {
            return CLIENT_TYPE.userAgent;
        }

        return originalUserAgent;
    }

    /**
     * Injection point.
     */
    public static boolean isClientSpoofingEnabled() {
        return SPOOF_CLIENT;
    }

    /**
     * Injection point.
     * <p>
     * When spoofing the client to iOS, the playback speed menu is missing from the player response.
     * This fix is required because playback speed is not available in YouTube Music Podcasts.
     * <p>
     * Return true to force create the playback speed menu.
     */
    public static boolean forceCreatePlaybackSpeedMenu(boolean original) {
        if (SPOOF_CLIENT) {
            return true;
        }
        return original;
    }
}