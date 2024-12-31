package app.revanced.extension.shared.patches.client;

import static app.revanced.extension.shared.patches.PatchStatus.SpoofStreamingDataMusic;

import android.os.Build;

import androidx.annotation.Nullable;

import app.revanced.extension.shared.settings.BaseSettings;

public class AppClient {
    // IOS
    /**
     * Video not playable: Paid / Movie / Private / Age-restricted
     * Note: Audio track available
     */
    private static final String PACKAGE_NAME_IOS = "com.google.ios.youtube";
    /**
     * The hardcoded client version of the iOS app used for InnerTube requests with this client.
     *
     * <p>
     * It can be extracted by getting the latest release version of the app on
     * <a href="https://apps.apple.com/us/app/youtube-watch-listen-stream/id544007664/">the App
     * Store page of the YouTube app</a>, in the {@code What’s New} section.
     * </p>
     */
    private static final String CLIENT_VERSION_IOS = forceAVC()
            ? "17.40.5"
            : "19.29.1";
    /**
     * The device machine id for the iPhone 15 Pro Max (iPhone16,2), used to get HDR with AV1 hardware decoding.
     *
     * <p>
     * See <a href="https://gist.github.com/adamawolf/3048717">this GitHub Gist</a> for more
     * information.
     * </p>
     */
    private static final String DEVICE_MODEL_IOS = forceAVC()
            ? "iPhone12,5"  // 11 Pro Max. (last device with iOS 13)
            : "iPhone16,2"; // 15 Pro Max.
    private static final String OS_VERSION_IOS = forceAVC()
            ? "13.7.17H35" // Last release of iOS 13.
            : "17.7.2.21H221";
    private static final String USER_AGENT_VERSION_IOS = forceAVC()
            ? "13_7"
            : "17_7_2";
    private static final String USER_AGENT_IOS =
            iOSUserAgent(PACKAGE_NAME_IOS, CLIENT_VERSION_IOS);


    // IOS UNPLUGGED
    /**
     * Video not playable: Paid / Movie
     * Note: Audio track available
     */
    private static final String PACKAGE_NAME_IOS_UNPLUGGED = "com.google.ios.youtubeunplugged";
    /**
     * The hardcoded client version of the iOS app used for InnerTube requests with this client.
     *
     * <p>
     * It can be extracted by getting the latest release version of the app on
     * <a href="https://apps.apple.com/us/app/youtube-tv/id1193350206/">the App
     * Store page of the YouTube TV app</a>, in the {@code What’s New} section.
     * </p>
     */
    private static final String CLIENT_VERSION_IOS_UNPLUGGED = forceAVC()
            ? "6.45"
            : "8.33";
    private static final String USER_AGENT_IOS_UNPLUGGED =
            iOSUserAgent(PACKAGE_NAME_IOS_UNPLUGGED, CLIENT_VERSION_IOS_UNPLUGGED);


    // IOS MUSIC
    /**
     * Video not playable: All videos that can't be played on YouTube Music
     */
    private static final String PACKAGE_NAME_IOS_MUSIC = "com.google.ios.youtubemusic";
    /**
     * The hardcoded client version of the iOS app used for InnerTube requests with this client.
     *
     * <p>
     * It can be extracted by getting the latest release version of the app on
     * <a href="https://apps.apple.com/us/app/youtube-music/id1017492454/">the App
     * Store page of the YouTube Music app</a>, in the {@code What’s New} section.
     * </p>
     */
    private static final String CLIENT_VERSION_IOS_MUSIC = "7.04";
    private static final String USER_AGENT_IOS_MUSIC =
            iOSUserAgent(PACKAGE_NAME_IOS_MUSIC, CLIENT_VERSION_IOS_MUSIC);


    // ANDROID VR
    /**
     * Video not playable: Kids
     * Note: Audio track is not available
     * <p>
     * Package name for YouTube VR (Google DayDream): com.google.android.apps.youtube.vr (Deprecated)
     * Package name for YouTube VR (Meta Quests): com.google.android.apps.youtube.vr.oculus
     * Package name for YouTube VR (ByteDance Pico 4): com.google.android.apps.youtube.vr.pico
     */
    private static final String PACKAGE_NAME_ANDROID_VR = "com.google.android.apps.youtube.vr.oculus";
    /**
     * The hardcoded client version of the Android VR app used for InnerTube requests with this client.
     *
     * <p>
     * It can be extracted by getting the latest release version of the app on
     * <a href="https://www.meta.com/en-us/experiences/2002317119880945/">the App
     * Store page of the YouTube app</a>, in the {@code Additional details} section.
     * </p>
     */
    private static final String CLIENT_VERSION_ANDROID_VR = "1.61.48";
    /**
     * The device machine id for the Meta Quest 3, used to get opus codec with the Android VR client.
     *
     * <p>
     * See <a href="https://dumps.tadiphone.dev/dumps/oculus/eureka">this GitLab</a> for more
     * information.
     * </p>
     */
    private static final String DEVICE_MODEL_ANDROID_VR = "Quest 3";
    private static final String OS_VERSION_ANDROID_VR = "12";
    /**
     * The SDK version for Android 12 is 31,
     * but for some reason the build.props for the {@code Quest 3} state that the SDK version is 32.
     */
    private static final String ANDROID_SDK_VERSION_ANDROID_VR = "32";
    private static final String USER_AGENT_ANDROID_VR =
            androidUserAgent(PACKAGE_NAME_ANDROID_VR, CLIENT_VERSION_ANDROID_VR, OS_VERSION_ANDROID_VR);


    // ANDROID UNPLUGGED
    /**
     * Video not playable: Playlists / Music
     * Note: Audio track is not available
     */
    private static final String PACKAGE_NAME_ANDROID_UNPLUGGED = "com.google.android.apps.youtube.unplugged";
    private static final String CLIENT_VERSION_ANDROID_UNPLUGGED = "8.16.0";
    /**
     * The device machine id for the Chromecast with Google TV 4K.
     *
     * <p>
     * See <a href="https://dumps.tadiphone.dev/dumps/google/kirkwood">this GitLab</a> for more
     * information.
     * </p>
     */
    private static final String DEVICE_MODEL_ANDROID_UNPLUGGED = "Google TV Streamer";
    private static final String OS_VERSION_ANDROID_UNPLUGGED = "14";
    private static final String ANDROID_SDK_VERSION_ANDROID_UNPLUGGED = "34";
    private static final String USER_AGENT_ANDROID_UNPLUGGED =
            androidUserAgent(PACKAGE_NAME_ANDROID_UNPLUGGED, CLIENT_VERSION_ANDROID_UNPLUGGED, OS_VERSION_ANDROID_UNPLUGGED);


    private AppClient() {
    }

    private static String androidUserAgent(String packageName, String clientVersion, String osVersion) {
        return packageName +
                "/" +
                clientVersion +
                " (Linux; U; Android " +
                osVersion +
                "; GB) gzip";
    }

    private static String iOSUserAgent(String packageName, String clientVersion) {
        return packageName +
                "/" +
                clientVersion +
                "(" +
                DEVICE_MODEL_IOS +
                "; U; CPU iOS " +
                USER_AGENT_VERSION_IOS +
                " like Mac OS X)";
    }

    public enum ClientType {
        ANDROID_VR(28,
                DEVICE_MODEL_ANDROID_VR,
                OS_VERSION_ANDROID_VR,
                USER_AGENT_ANDROID_VR,
                ANDROID_SDK_VERSION_ANDROID_VR,
                CLIENT_VERSION_ANDROID_VR,
                true,
                "Android VR"
        ),
        ANDROID_UNPLUGGED(29,
                DEVICE_MODEL_ANDROID_UNPLUGGED,
                OS_VERSION_ANDROID_UNPLUGGED,
                USER_AGENT_ANDROID_UNPLUGGED,
                ANDROID_SDK_VERSION_ANDROID_UNPLUGGED,
                CLIENT_VERSION_ANDROID_UNPLUGGED,
                true,
                "Android TV"
        ),
        IOS_UNPLUGGED(33,
                DEVICE_MODEL_IOS,
                OS_VERSION_IOS,
                USER_AGENT_IOS_UNPLUGGED,
                null,
                CLIENT_VERSION_IOS_UNPLUGGED,
                true,
                forceAVC()
                        ? "iOS TV Force AVC"
                        : "iOS TV"
        ),
        IOS(5,
                DEVICE_MODEL_IOS,
                OS_VERSION_IOS,
                USER_AGENT_IOS,
                null,
                CLIENT_VERSION_IOS,
                false,
                forceAVC()
                        ? "iOS Force AVC"
                        : "iOS"
        ),
        IOS_MUSIC(
                26,
                DEVICE_MODEL_IOS,
                OS_VERSION_IOS,
                USER_AGENT_IOS_MUSIC,
                null,
                CLIENT_VERSION_IOS_MUSIC,
                true,
                "iOS Music"
        );

        /**
         * YouTube
         * <a href="https://github.com/zerodytrash/YouTube-Internal-Clients?tab=readme-ov-file#clients">client type</a>
         */
        public final int id;

        public final String clientName;

        /**
         * Device model, equivalent to {@link Build#MODEL} (System property: ro.product.model)
         */
        public final String deviceModel;

        /**
         * Device OS version.
         */
        public final String osVersion;

        /**
         * Player user-agent.
         */
        public final String userAgent;

        /**
         * Android SDK version, equivalent to {@link Build.VERSION#SDK} (System property: ro.build.version.sdk)
         * Field is null if not applicable.
         */
        @Nullable
        public final String androidSdkVersion;

        /**
         * App version.
         */
        public final String clientVersion;

        /**
         * If the client can access the API logged in.
         */
        public final boolean canLogin;

        /**
         * Friendly name displayed in stats for nerds.
         */
        public final String friendlyName;

        ClientType(int id,
                   String deviceModel,
                   String osVersion,
                   String userAgent,
                   @Nullable String androidSdkVersion,
                   String clientVersion,
                   boolean canLogin,
                   String friendlyName
        ) {
            this.id = id;
            this.clientName = name();
            this.deviceModel = deviceModel;
            this.clientVersion = clientVersion;
            this.osVersion = osVersion;
            this.androidSdkVersion = androidSdkVersion;
            this.userAgent = userAgent;
            this.canLogin = canLogin;
            this.friendlyName = friendlyName;
        }

        private static final ClientType[] CLIENT_ORDER_TO_USE_YOUTUBE = {
                ANDROID_VR,
                ANDROID_UNPLUGGED,
                IOS_UNPLUGGED,
                IOS,
        };

        private static final ClientType[] CLIENT_ORDER_TO_USE_YOUTUBE_MUSIC = {
                ANDROID_VR,
                IOS_MUSIC,
        };
    }

    private static boolean forceAVC() {
        return BaseSettings.SPOOF_STREAMING_DATA_IOS_FORCE_AVC.get();
    }

    public static ClientType[] getAvailableClientTypes() {
        return SpoofStreamingDataMusic()
                ? ClientType.CLIENT_ORDER_TO_USE_YOUTUBE_MUSIC
                : ClientType.CLIENT_ORDER_TO_USE_YOUTUBE;
    }
}
