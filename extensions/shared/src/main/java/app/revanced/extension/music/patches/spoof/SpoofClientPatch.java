package app.revanced.extension.music.patches.spoof;

import app.revanced.extension.music.settings.Settings;

@SuppressWarnings("unused")
public class SpoofClientPatch extends BlockRequestPatch {
    private static final boolean SETTINGS_INITIALIZED = Settings.SETTINGS_INITIALIZED.get();

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
    public static String getOSName(String original) {
        if (SPOOF_CLIENT) {
            return CLIENT_TYPE.osName;
        }

        return original;
    }

    /**
     * Injection point.
     */
    public static String getOSVersion(String original) {
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
     * <p>
     * This function performs the same function as the 'Spoof app version' patch.
     * To make it work without the 'Spoof app version' patch, it is hooked into another method.
     * <p>
     * The app version is spoofed to 6.35.52 only when the app is first installed.
     * After the app is restarted, the app version is no longer spoofed.
     * <p>
     * This simple operation fixes the issue where the video action bar is not shown.
     */
    public static String getClientVersionOverride(String version) {
        if (SPOOF_CLIENT && !SETTINGS_INITIALIZED) {
            return "6.35.52";
        }

        return version;
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
     * Return false to force disable fallback feature flag.
     */
    public static boolean forceDisableFallbackFeatureFlag(boolean original) {
        if (SPOOF_CLIENT) {
            return false;
        }
        return original;
    }

    /**
     * Injection point.
     * <p>
     * Return false to force disable formats feature flag.
     */
    public static boolean forceDisableFormatsFeatureFlag(boolean original) {
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