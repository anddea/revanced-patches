package app.revanced.extension.music.patches.misc.client;

import android.os.Build;

import androidx.annotation.Nullable;

public class AppClient {

    /**
     * The hardcoded client version of the iOS app used for InnerTube requests with this client.
     *
     * <p>
     * It can be extracted by getting the latest release version of the app on
     * <a href="https://apps.apple.com/us/app/music-watch-listen-stream/id544007664/">the App
     * Store page of the YouTube app</a>, in the {@code What’s New} section.
     * </p>
     */
    private static final String CLIENT_VERSION_IOS = "6.21";
    private static final String DEVICE_MAKE_IOS = "Apple";
    /**
     * See <a href="https://gist.github.com/adamawolf/3048717">this GitHub Gist</a> for more
     * information.
     * </p>
     */
    private static final String DEVICE_MODEL_IOS = "iPhone16,2";
    private static final String OS_NAME_IOS = "iOS";
    private static final String OS_VERSION_IOS = "17.7.2.21H221";
    private static final String USER_AGENT_VERSION_IOS = "17_7_2";
    private static final String USER_AGENT_IOS = "com.google.ios.youtubemusic/" +
            CLIENT_VERSION_IOS +
            "(" +
            DEVICE_MODEL_IOS +
            "; U; CPU iOS " +
            USER_AGENT_VERSION_IOS +
            " like Mac OS X)";

    private AppClient() {
    }

    public enum ClientType {
        IOS_MUSIC(26,
                DEVICE_MAKE_IOS,
                DEVICE_MODEL_IOS,
                CLIENT_VERSION_IOS,
                OS_NAME_IOS,
                OS_VERSION_IOS,
                null,
                USER_AGENT_IOS,
                true
        );

        /**
         * YouTube
         * <a href="https://github.com/zerodytrash/YouTube-Internal-Clients?tab=readme-ov-file#clients">client type</a>
         */
        public final int id;

        /**
         * Device manufacturer.
         */
        @Nullable
        public final String deviceMake;

        /**
         * Device model, equivalent to {@link Build#MODEL} (System property: ro.product.model)
         */
        public final String deviceModel;

        /**
         * Device OS name.
         */
        @Nullable
        public final String osName;

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
        public final Integer androidSdkVersion;

        /**
         * App version.
         */
        public final String clientVersion;

        /**
         * If the client can access the API logged in.
         */
        public final boolean canLogin;

        ClientType(int id,
                   @Nullable String deviceMake,
                   String deviceModel,
                   String clientVersion,
                   @Nullable String osName,
                   String osVersion,
                   Integer androidSdkVersion,
                   String userAgent,
                   boolean canLogin
        ) {
            this.id = id;
            this.deviceMake = deviceMake;
            this.deviceModel = deviceModel;
            this.clientVersion = clientVersion;
            this.osName = osName;
            this.osVersion = osVersion;
            this.androidSdkVersion = androidSdkVersion;
            this.userAgent = userAgent;
            this.canLogin = canLogin;
        }
    }
}
