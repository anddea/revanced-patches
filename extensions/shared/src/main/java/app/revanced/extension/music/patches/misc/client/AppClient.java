package app.revanced.extension.music.patches.misc.client;

import android.os.Build;

public class AppClient {

    // Audio codec is MP4A.
    private static final String CLIENT_VERSION_ANDROID_MUSIC_4_27 = "4.27.53";

    // Audio codec is OPUS.
    private static final String CLIENT_VERSION_ANDROID_MUSIC_5_29 = "5.29.53";

    private static final String PACKAGE_NAME_ANDROID_MUSIC = "com.google.android.apps.youtube.music";
    private static final String DEVICE_MODEL_ANDROID_MUSIC = Build.MODEL;
    private static final String OS_VERSION_ANDROID_MUSIC = Build.VERSION.RELEASE;

    // Audio codec is MP4A.
    private static final String CLIENT_VERSION_IOS_MUSIC_6_21 = "6.21";

    // Audio codec is OPUS.
    private static final String CLIENT_VERSION_IOS_MUSIC_7_04 = "7.04";

    private static final String PACKAGE_NAME_IOS_MUSIC = "com.google.ios.youtubemusic";
    private static final String DEVICE_MODEL_IOS_MUSIC = "iPhone14,3";
    private static final String OS_VERSION_IOS_MUSIC = "15.7.1.19H117";
    private static final String USER_AGENT_VERSION_IOS_MUSIC = "15_7_1";

    private AppClient() {
    }

    private static String androidUserAgent(String clientVersion) {
        return PACKAGE_NAME_ANDROID_MUSIC +
                "/" +
                clientVersion +
                " (Linux; U; Android " +
                OS_VERSION_ANDROID_MUSIC +
                "; GB) gzip";
    }

    private static String iOSUserAgent(String clientVersion) {
        return PACKAGE_NAME_IOS_MUSIC +
                "/" +
                clientVersion +
                "(" +
                DEVICE_MODEL_IOS_MUSIC +
                "; U; CPU iOS " +
                USER_AGENT_VERSION_IOS_MUSIC +
                " like Mac OS X)";
    }

    public enum ClientType {
        ANDROID_MUSIC_4_27(21,
                DEVICE_MODEL_ANDROID_MUSIC,
                OS_VERSION_ANDROID_MUSIC,
                androidUserAgent(CLIENT_VERSION_ANDROID_MUSIC_4_27),
                CLIENT_VERSION_ANDROID_MUSIC_4_27
        ),
        ANDROID_MUSIC_5_29(21,
                DEVICE_MODEL_ANDROID_MUSIC,
                OS_VERSION_ANDROID_MUSIC,
                androidUserAgent(CLIENT_VERSION_ANDROID_MUSIC_5_29),
                CLIENT_VERSION_ANDROID_MUSIC_5_29
        ),
        IOS_MUSIC_6_21(
                26,
                DEVICE_MODEL_IOS_MUSIC,
                OS_VERSION_IOS_MUSIC,
                iOSUserAgent(CLIENT_VERSION_IOS_MUSIC_6_21),
                CLIENT_VERSION_IOS_MUSIC_6_21
        ),
        IOS_MUSIC_7_04(
                26,
                DEVICE_MODEL_IOS_MUSIC,
                OS_VERSION_IOS_MUSIC,
                iOSUserAgent(CLIENT_VERSION_IOS_MUSIC_7_04),
                CLIENT_VERSION_IOS_MUSIC_7_04
        );

        /**
         * YouTube
         * <a href="https://github.com/zerodytrash/YouTube-Internal-Clients?tab=readme-ov-file#clients">client type</a>
         */
        public final int id;

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
         * App version.
         */
        public final String clientVersion;

        ClientType(int id,
                   String deviceModel,
                   String osVersion,
                   String userAgent,
                   String clientVersion
        ) {
            this.id = id;
            this.deviceModel = deviceModel;
            this.clientVersion = clientVersion;
            this.osVersion = osVersion;
            this.userAgent = userAgent;
        }
    }
}
