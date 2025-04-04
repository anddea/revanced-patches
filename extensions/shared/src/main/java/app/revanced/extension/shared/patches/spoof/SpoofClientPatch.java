package app.revanced.extension.shared.patches.spoof;

import app.revanced.extension.shared.innertube.client.YouTubeMusicAppClient.ClientType;
import app.revanced.extension.shared.settings.BaseSettings;

@SuppressWarnings("unused")
public class SpoofClientPatch extends BlockRequestPatch {
    private static final ClientType CLIENT_TYPE = BaseSettings.SPOOF_CLIENT_TYPE.get();

    /**
     * Injection point.
     */
    public static int getClientId(int original) {
        if (SPOOF_CLIENT) {
            return CLIENT_TYPE.id;
        }

        return original;
    }

    /**
     * Injection point.
     */
    public static String getClientVersion(String original) {
        if (SPOOF_CLIENT) {
            return CLIENT_TYPE.clientVersion;
        }

        return original;
    }

    /**
     * Injection point.
     */
    public static String getDeviceBrand(String original) {
        if (SPOOF_CLIENT) {
            return CLIENT_TYPE.deviceBrand;
        }

        return original;
    }

    /**
     * Injection point.
     */
    public static String getDeviceMake(String original) {
        if (SPOOF_CLIENT) {
            return CLIENT_TYPE.deviceMake;
        }

        return original;
    }

    /**
     * Injection point.
     */
    public static String getDeviceModel(String original) {
        if (SPOOF_CLIENT) {
            return CLIENT_TYPE.deviceModel;
        }

        return original;
    }

    /**
     * Injection point.
     */
    public static String getOsName(String original) {
        if (SPOOF_CLIENT) {
            return CLIENT_TYPE.osName;
        }

        return original;
    }

    /**
     * Injection point.
     */
    public static String getOsVersion(String original) {
        if (SPOOF_CLIENT) {
            return CLIENT_TYPE.osVersion;
        }

        return original;
    }

    /**
     * Injection point.
     */
    public static String getUserAgent(String original) {
        if (SPOOF_CLIENT) {
            return CLIENT_TYPE.userAgent;
        }

        return original;
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

    /**
     * Injection point.
     * <p>
     * When spoofing the client to Android, the playback speed menu is missing from the player response.
     * This fix is required because playback speed is not available in YouTube Music Podcasts.
     * <p>
     * Return false to force create the playback speed menu.
     */
    public static boolean forceCreatePlaybackSpeedMenuInverse(boolean original) {
        if (SPOOF_CLIENT) {
            return false;
        }
        return original;
    }

    /**
     * Injection point.
     * <p>
     * Return false to force disable playback feature flag.
     */
    public static boolean forceDisablePlaybackFeatureFlag(boolean original) {
        if (SPOOF_CLIENT) {
            return false;
        }
        return original;
    }
}