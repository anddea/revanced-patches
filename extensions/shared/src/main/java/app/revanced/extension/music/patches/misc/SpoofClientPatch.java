package app.revanced.extension.music.patches.misc;

import app.revanced.extension.shared.patches.BlockRequestPatch;

@SuppressWarnings("unused")
public class SpoofClientPatch extends BlockRequestPatch {
    private static final int CLIENT_ID_IOS_MUSIC = 26;

    /**
     * 1. {@link BlockRequestPatch} is not required, as the litho component is not applied to the video action bar and flyout menu.
     * 2. Audio codec is MP4A.
     */
    private static final String CLIENT_VERSION_IOS_MUSIC_6_21 = "6.21";

    /**
     * 1. {@link BlockRequestPatch} is required, as the layout used in iOS should be prevented from being applied to the video action bar and flyout menu.
     * 2. Audio codec is OPUS.
     */
    private static final String CLIENT_VERSION_IOS_MUSIC_7_31 = "7.31.2";

    /**
     * Starting with YouTube Music 7.17.51+, the litho component has been applied to the video action bar.
     * <p>
     * So if {@code CLIENT_VERSION_IOS_MUSIC_6_21} is used in YouTube Music 7.17.51+,
     * the video action bar will not load properly.
     */
    private static final String CLIENT_VERSION_IOS_MUSIC = IS_7_17_OR_GREATER
            ? CLIENT_VERSION_IOS_MUSIC_6_21
            : CLIENT_VERSION_IOS_MUSIC_7_31;
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

    /**
     * Injection point.
     */
    public static int getClientTypeId(int originalClientTypeId) {
        if (SPOOF_CLIENT) {
            return CLIENT_ID_IOS_MUSIC;
        }

        return originalClientTypeId;
    }

    /**
     * Injection point.
     */
    public static String getClientVersion(String originalClientVersion) {
        if (SPOOF_CLIENT) {
            return CLIENT_VERSION_IOS_MUSIC;
        }

        return originalClientVersion;
    }

    /**
     * Injection point.
     */
    public static String getClientModel(String originalClientModel) {
        if (SPOOF_CLIENT) {
            return DEVICE_MODEL_IOS_MUSIC;
        }

        return originalClientModel;
    }

    /**
     * Injection point.
     */
    public static String getOsVersion(String originalOsVersion) {
        if (SPOOF_CLIENT) {
            return OS_VERSION_IOS_MUSIC;
        }

        return originalOsVersion;
    }

    /**
     * Injection point.
     */
    public static String getUserAgent(String originalUserAgent) {
        if (SPOOF_CLIENT) {
            return USER_AGENT_IOS_MUSIC;
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