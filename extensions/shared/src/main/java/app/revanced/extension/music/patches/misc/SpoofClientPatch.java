package app.revanced.extension.music.patches.misc;

import app.revanced.extension.music.settings.Settings;

@SuppressWarnings("unused")
public class SpoofClientPatch {
    private static final int CLIENT_ID_IOS_MUSIC = 26;
    /**
     * The hardcoded client version of the iOS app used for InnerTube requests with this client.
     *
     * <p>
     * It can be extracted by getting the latest release version of the app on
     * <a href="https://apps.apple.com/us/app/youtube-music/id1017492454/">the App
     * Store page of the YouTube app</a>, in the {@code What¡¯s New} section.
     * </p>
     */
    private static final String CLIENT_VERSION_IOS_MUSIC = "6.21";
    /**
     * See <a href="https://gist.github.com/adamawolf/3048717">this GitHub Gist</a> for more
     * information.
     * </p>
     */
    private static final String DEVICE_MODEL_IOS_MUSIC = "iPhone16,2";
    private static final String OS_VERSION_IOS_MUSIC = "17.7.2.21H221";
    private static final String USER_AGENT_VERSION_IOS_MUSIC = "17_7_2";
    private static final String USER_AGENT_IOS_MUSIC = "com.google.ios.youtubemusic/" +
            CLIENT_VERSION_IOS_MUSIC +
            "(" +
            DEVICE_MODEL_IOS_MUSIC +
            "; U; CPU iOS " +
            USER_AGENT_VERSION_IOS_MUSIC +
            " like Mac OS X)";

    private static final boolean SPOOF_CLIENT_ENABLED = Settings.SPOOF_CLIENT.get();

    /**
     * Injection point.
     */
    public static int getClientTypeId(int originalClientTypeId) {
        if (SPOOF_CLIENT_ENABLED) {
            return CLIENT_ID_IOS_MUSIC;
        }

        return originalClientTypeId;
    }

    /**
     * Injection point.
     */
    public static String getClientVersion(String originalClientVersion) {
        if (SPOOF_CLIENT_ENABLED) {
            return CLIENT_VERSION_IOS_MUSIC;
        }

        return originalClientVersion;
    }

    /**
     * Injection point.
     */
    public static String getClientModel(String originalClientModel) {
        if (SPOOF_CLIENT_ENABLED) {
            return DEVICE_MODEL_IOS_MUSIC;
        }

        return originalClientModel;
    }

    /**
     * Injection point.
     */
    public static String getOsVersion(String originalOsVersion) {
        if (SPOOF_CLIENT_ENABLED) {
            return OS_VERSION_IOS_MUSIC;
        }

        return originalOsVersion;
    }

    /**
     * Injection point.
     */
    public static String getUserAgent(String originalUserAgent) {
        if (SPOOF_CLIENT_ENABLED) {
            return USER_AGENT_IOS_MUSIC;
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