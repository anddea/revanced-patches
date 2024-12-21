package app.revanced.extension.music.patches.misc.client;

import android.os.Build;

import app.revanced.extension.music.settings.Settings;

public class AppClient {
    private static final String CLIENT_VERSION_ANDROID_MUSIC = Settings.SPOOF_CLIENT_LEGACY.get()
            ? "4.27.53"     // Audio codec is MP4A.
            : "5.29.53";    // Audio codec is OPUS.
    private static final String OS_VERSION_ANDROID_MUSIC = Build.VERSION.RELEASE;
    private static final String USER_AGENT_ANDROID_MUSIC = "com.google.android.apps.youtube.music/" +
            CLIENT_VERSION_ANDROID_MUSIC +
            " (Linux; U; Android " +
            OS_VERSION_ANDROID_MUSIC +
            "; GB) gzip";

    private static final String CLIENT_VERSION_IOS_MUSIC = Settings.SPOOF_CLIENT_LEGACY.get()
            ? "4.27"        // Audio codec is MP4A.
            : "7.31.2";     // Audio codec is OPUS.
    private static final String DEVICE_MODEL_IOS_MUSIC = "iPhone14,3";
    private static final String OS_VERSION_IOS_MUSIC = "15.7.1.19H117";
    private static final String USER_AGENT_VERSION_IOS_MUSIC = "15_7_1";
    private static final String USER_AGENT_IOS_MUSIC = "com.google.ios.youtubemusic/" +
            CLIENT_VERSION_IOS_MUSIC +
            "(" +
            DEVICE_MODEL_IOS_MUSIC +
            "; U; CPU iOS " +
            USER_AGENT_VERSION_IOS_MUSIC +
            " like Mac OS X)";

    private AppClient() {
    }

    public enum ClientType {
        ANDROID_MUSIC(21,
                Build.MODEL,
                OS_VERSION_ANDROID_MUSIC,
                USER_AGENT_ANDROID_MUSIC,
                CLIENT_VERSION_ANDROID_MUSIC
        ),
        IOS_MUSIC(
                26,
                DEVICE_MODEL_IOS_MUSIC,
                OS_VERSION_IOS_MUSIC,
                USER_AGENT_IOS_MUSIC,
                CLIENT_VERSION_IOS_MUSIC
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
