package app.revanced.extension.shared.innertube.client;

import android.os.Build;

import java.util.Locale;

public class YouTubeMusicAppClient {

    // Response to the '/next' request is 'Please update to continue using the app':
    // https://github.com/inotia00/ReVanced_Extended/issues/2743
    // Nevertheless, '/player' request is still valid.

    // Audio codec is MP4A.
    private static final String CLIENT_VERSION_ANDROID_MUSIC_4_27 = "4.27.53";

    // Audio codec is OPUS.
    private static final String CLIENT_VERSION_ANDROID_MUSIC_5_29 = "5.29.53";

    private static final String PACKAGE_NAME_ANDROID_MUSIC = "com.google.android.apps.youtube.music";
    private static final String DEVICE_BRAND_ANDROID_MUSIC = Build.BRAND;
    private static final String DEVICE_MAKE_ANDROID_MUSIC = Build.MANUFACTURER;
    private static final String DEVICE_MODEL_ANDROID_MUSIC = Build.MODEL;
    private static final String BUILD_ID_ANDROID_MUSIC = Build.ID;
    // In YouTube, this OS name is used to hide ads in Shorts.
    private static final String OS_NAME_ANDROID_MUSIC = "Android Automotive";
    private static final String OS_VERSION_ANDROID_MUSIC = Build.VERSION.RELEASE;

    // Audio codec is MP4A.
    private static final String CLIENT_VERSION_IOS_MUSIC_6_21 = "6.21";
    private static final String DEVICE_MODEL_IOS_MUSIC_6_21 = "iPhone14,3";
    private static final String OS_VERSION_IOS_MUSIC_6_21 = "15.7.1.19H117";
    private static final String USER_AGENT_VERSION_IOS_MUSIC_6_21 = "15_7_1";

    // Audio codec is OPUS.
    private static final String CLIENT_VERSION_IOS_MUSIC_7_04 = "7.04";
    // Release date for iOS YouTube Music 7.04 is June 4, 2024.
    // Release date for iOS 18 is September 16, 2024.
    // Since iOS cannot downgrade the OS version or app version in the usual way,
    // 17.7.2 is used as an iOS version that matches iOS YouTube Music 7.04.
    private static final String DEVICE_MODEL_IOS_MUSIC_7_04 = "iPhone16,2";
    private static final String OS_VERSION_IOS_MUSIC_7_04 = "17.7.2.21H221";
    private static final String USER_AGENT_VERSION_IOS_MUSIC_7_04 = "17_7_2";

    private static final String PACKAGE_NAME_IOS_MUSIC = "com.google.ios.youtubemusic";
    private static final String DEVICE_BRAND_IOS_MUSIC = "Apple";
    private static final String DEVICE_MAKE_IOS_MUSIC = "Apple";
    private static final String OS_NAME_IOS_MUSIC = "iOS";

    private YouTubeMusicAppClient() {
    }

    private static String androidUserAgent(String clientVersion) {
        return PACKAGE_NAME_ANDROID_MUSIC +
                "/" +
                clientVersion +
                "(Linux; U; Android " +
                OS_VERSION_ANDROID_MUSIC +
                "; " +
                Locale.getDefault() +
                "; " +
                DEVICE_MODEL_ANDROID_MUSIC +
                " Build/" +
                BUILD_ID_ANDROID_MUSIC +
                ") gzip";
    }

    private static String iOSUserAgent(String clientVersion, String deviceModel, String osVersion) {
        return PACKAGE_NAME_IOS_MUSIC +
                "/" +
                clientVersion +
                " (" +
                deviceModel +
                "; U; CPU iOS " +
                osVersion +
                " like Mac OS X; " +
                Locale.getDefault() +
                ")";
    }

    public enum ActionButtonType {
        NONE,           // No action button (~ 6.14)
        YOUTUBE_BUTTON, // Type of action button is YouTubeButton (6.15 ~ 7.16)
        LITHO           // Type of action button is ComponentHost (7.17 ~)
    }

    public enum ClientType {
        ANDROID_MUSIC_4_27(21,
                DEVICE_BRAND_ANDROID_MUSIC,
                DEVICE_MAKE_ANDROID_MUSIC,
                DEVICE_MODEL_ANDROID_MUSIC,
                OS_NAME_ANDROID_MUSIC,
                OS_VERSION_ANDROID_MUSIC,
                androidUserAgent(CLIENT_VERSION_ANDROID_MUSIC_4_27),
                CLIENT_VERSION_ANDROID_MUSIC_4_27,
                ActionButtonType.NONE
        ),
        ANDROID_MUSIC_5_29(21,
                DEVICE_BRAND_ANDROID_MUSIC,
                DEVICE_MAKE_ANDROID_MUSIC,
                DEVICE_MODEL_ANDROID_MUSIC,
                OS_NAME_ANDROID_MUSIC,
                OS_VERSION_ANDROID_MUSIC,
                androidUserAgent(CLIENT_VERSION_ANDROID_MUSIC_5_29),
                CLIENT_VERSION_ANDROID_MUSIC_5_29,
                ActionButtonType.NONE
        ),
        IOS_MUSIC_6_21(26,
                DEVICE_BRAND_IOS_MUSIC,
                DEVICE_MAKE_IOS_MUSIC,
                DEVICE_MODEL_IOS_MUSIC_6_21,
                OS_NAME_IOS_MUSIC,
                OS_VERSION_IOS_MUSIC_6_21,
                iOSUserAgent(CLIENT_VERSION_IOS_MUSIC_6_21, DEVICE_MODEL_IOS_MUSIC_6_21, USER_AGENT_VERSION_IOS_MUSIC_6_21),
                CLIENT_VERSION_IOS_MUSIC_6_21,
                ActionButtonType.YOUTUBE_BUTTON
        ),
        IOS_MUSIC_7_04(26,
                DEVICE_BRAND_IOS_MUSIC,
                DEVICE_MAKE_IOS_MUSIC,
                DEVICE_MODEL_IOS_MUSIC_7_04,
                OS_NAME_IOS_MUSIC,
                OS_VERSION_IOS_MUSIC_7_04,
                iOSUserAgent(CLIENT_VERSION_IOS_MUSIC_7_04, DEVICE_MODEL_IOS_MUSIC_7_04, USER_AGENT_VERSION_IOS_MUSIC_7_04),
                CLIENT_VERSION_IOS_MUSIC_7_04,
                ActionButtonType.LITHO
        );

        /**
         * YouTube
         * <a href="https://github.com/zerodytrash/YouTube-Internal-Clients?tab=readme-ov-file#clients">client type</a>
         */
        public final int id;

        /**
         * Device brand, equivalent to {@link Build#BRAND} (System property: ro.product.vendor.brand)
         */
        public final String deviceBrand;

        /**
         * Device make, equivalent to {@link Build#MANUFACTURER} (System property: ro.product.vendor.manufacturer)
         */
        public final String deviceMake;

        /**
         * Device model, equivalent to {@link Build#MODEL} (System property: ro.product.model)
         */
        public final String deviceModel;

        /**
         * Device OS name.
         */
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
         * App version.
         */
        public final String clientVersion;

        private final ActionButtonType actionButtonType;

        ClientType(int id,
                   String deviceBrand,
                   String deviceMake,
                   String deviceModel,
                   String osName,
                   String osVersion,
                   String userAgent,
                   String clientVersion,
                   ActionButtonType actionButtonType
        ) {
            this.id = id;
            this.deviceBrand = deviceBrand;
            this.deviceMake = deviceMake;
            this.deviceModel = deviceModel;
            this.clientVersion = clientVersion;
            this.osName = osName;
            this.osVersion = osVersion;
            this.userAgent = userAgent;
            this.actionButtonType = actionButtonType;
        }

        public boolean isYouTubeButton() {
            return actionButtonType == ActionButtonType.YOUTUBE_BUTTON;
        }
    }
}
