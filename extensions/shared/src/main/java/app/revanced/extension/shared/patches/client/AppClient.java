package app.revanced.extension.shared.patches.client;

import static app.revanced.extension.shared.utils.ResourceUtils.getString;

import android.os.Build;

import androidx.annotation.Nullable;

public class AppClient {
    // IOS
    /**
     * The hardcoded client version of the iOS app used for InnerTube requests with this client.
     *
     * <p>
     * It can be extracted by getting the latest release version of the app on
     * <a href="https://apps.apple.com/us/app/youtube-watch-listen-stream/id544007664/">the App
     * Store page of the YouTube app</a>, in the {@code What’s New} section.
     * </p>
     */
    private static final String CLIENT_VERSION_IOS = "19.49.5";
    /**
     * The device machine id for the iPhone 16 Pro Max (iPhone17,2), used to get HDR with AV1 hardware decoding.
     *
     * <p>
     * See <a href="https://gist.github.com/adamawolf/3048717">this GitHub Gist</a> for more
     * information.
     * </p>
     */
    private static final String DEVICE_MODEL_IOS = "iPhone17,2";
    /**
     * The minimum supported OS version for the iOS YouTube client is iOS 14.0.
     * Using an invalid OS version will use the AVC codec.
     */
    private static final String OS_VERSION_IOS = "18.1.1.22B91";
    private static final String USER_AGENT_VERSION_IOS = "18_1_1";
    private static final String USER_AGENT_IOS = "com.google.ios.youtube/" +
            CLIENT_VERSION_IOS +
            "(" +
            DEVICE_MODEL_IOS +
            "; U; CPU iOS " +
            USER_AGENT_VERSION_IOS +
            " like Mac OS X)";

    // IOS MUSIC
    /**
     * The hardcoded client version of the iOS app used for InnerTube requests with this client.
     *
     * <p>
     * It can be extracted by getting the latest release version of the app on
     * <a href="https://apps.apple.com/us/app/youtube-music/id1017492454/">the App
     * Store page of the YouTube Music app</a>, in the {@code What’s New} section.
     * </p>
     */
    private static final String CLIENT_VERSION_IOS_MUSIC = "7.31.2";
    private static final String USER_AGENT_IOS_MUSIC = "com.google.ios.youtubemusic/" +
            CLIENT_VERSION_IOS_MUSIC +
            "(" +
            DEVICE_MODEL_IOS +
            "; U; CPU iOS " +
            USER_AGENT_VERSION_IOS +
            " like Mac OS X)";

    // ANDROID VR
    /**
     * The hardcoded client version of the Android VR app used for InnerTube requests with this client.
     *
     * <p>
     * It can be extracted by getting the latest release version of the app on
     * <a href="https://www.meta.com/en-us/experiences/2002317119880945/">the App
     * Store page of the YouTube app</a>, in the {@code Additional details} section.
     * </p>
     */
    private static final String CLIENT_VERSION_ANDROID_VR = "1.61.47";
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
    /**
     * Package name for YouTube VR (Google DayDream): com.google.android.apps.youtube.vr (Deprecated)
     * Package name for YouTube VR (Meta Quests): com.google.android.apps.youtube.vr.oculus
     * Package name for YouTube VR (ByteDance Pico 4): com.google.android.apps.youtube.vr.pico
     */
    private static final String USER_AGENT_ANDROID_VR = "com.google.android.apps.youtube.vr.oculus/" +
            CLIENT_VERSION_ANDROID_VR +
            " (Linux; U; Android " +
            OS_VERSION_ANDROID_VR +
            "; GB) gzip";

    // ANDROID UNPLUGGED
    private static final String CLIENT_VERSION_ANDROID_UNPLUGGED = "8.49.0";
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
    private static final String USER_AGENT_ANDROID_UNPLUGGED = "com.google.android.apps.youtube.unplugged/" +
            CLIENT_VERSION_ANDROID_UNPLUGGED +
            " (Linux; U; Android " +
            OS_VERSION_ANDROID_UNPLUGGED +
            "; GB) gzip";

    private AppClient() {
    }

    public enum ClientType {
        IOS(5,
                DEVICE_MODEL_IOS,
                OS_VERSION_IOS,
                USER_AGENT_IOS,
                null,
                CLIENT_VERSION_IOS,
                false
        ),
        ANDROID_VR(28,
                DEVICE_MODEL_ANDROID_VR,
                OS_VERSION_ANDROID_VR,
                USER_AGENT_ANDROID_VR,
                ANDROID_SDK_VERSION_ANDROID_VR,
                CLIENT_VERSION_ANDROID_VR,
                true
        ),
        ANDROID_UNPLUGGED(29,
                DEVICE_MODEL_ANDROID_UNPLUGGED,
                OS_VERSION_ANDROID_UNPLUGGED,
                USER_AGENT_ANDROID_UNPLUGGED,
                ANDROID_SDK_VERSION_ANDROID_UNPLUGGED,
                CLIENT_VERSION_ANDROID_UNPLUGGED,
                true
        ),
        IOS_MUSIC(
                26,
                DEVICE_MODEL_IOS,
                OS_VERSION_IOS,
                USER_AGENT_IOS_MUSIC,
                null,
                CLIENT_VERSION_IOS_MUSIC,
                true
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

        ClientType(int id,
                   String deviceModel,
                   String osVersion,
                   String userAgent,
                   @Nullable String androidSdkVersion,
                   String clientVersion,
                   boolean canLogin
        ) {
            this.id = id;
            this.clientName = name();
            this.deviceModel = deviceModel;
            this.clientVersion = clientVersion;
            this.osVersion = osVersion;
            this.androidSdkVersion = androidSdkVersion;
            this.userAgent = userAgent;
            this.canLogin = canLogin;
        }

        public final String getFriendlyName() {
            return getString("revanced_spoof_streaming_data_type_entry_" + name().toLowerCase());
        }
    }
}
